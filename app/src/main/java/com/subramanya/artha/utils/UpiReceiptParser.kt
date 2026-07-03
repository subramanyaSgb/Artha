package com.subramanya.artha.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.subramanya.artha.utils.upi.PhonePeReceiptParser
import com.subramanya.artha.utils.upi.UpiParsedReceipt
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Parses a UPI payment receipt image into structured fields.
 *
 * Primary path (when [keyProvider] is set): sends the image to NVIDIA NIM (z-ai/glm-5.2)
 * which handles any UPI app layout — PhonePe, GPay, Paytm, etc.
 *
 * Fallback path (no key, or NIM fails): on-device ML Kit OCR + per-app regex.
 * Currently the regex only covers PhonePe; add more parsers in the fallback block as needed.
 */
class UpiReceiptParser(
    private val keyProvider: (suspend () -> String)? = null,
) {

    suspend fun parse(context: Context, uri: Uri): UpiParsedReceipt? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        // Try NIM vision first when a key is configured
        val key = keyProvider?.invoke()?.takeIf { it.isNotBlank() }
        if (key != null) {
            runCatching { parseWithNim(key, bitmap) }.getOrNull()?.let { return it }
        }

        // Fallback: ML Kit OCR → regex
        val text = runOcr(bitmap)
        return PhonePeReceiptParser.parse(text)
    }

    private suspend fun parseWithNim(key: String, bitmap: Bitmap): UpiParsedReceipt? {
        val b64 = bitmapToBase64(bitmap)
        val body = """
            {
              "model": "z-ai/glm-5.2",
              "messages": [{
                "role": "user",
                "content": [
                  {"type": "text", "text": ${nimPrompt()}},
                  {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,$b64"}}
                ]
              }],
              "temperature": 0.1,
              "max_tokens": 256,
              "stream": false
            }
        """.trimIndent()

        val raw = post(key, body)
        val content = extractContent(raw) ?: return null
        val json = extractJsonObject(content) ?: return null
        return decodeReceipt(json)
    }

    private fun nimPrompt(): String {
        // JSON-encoded string literal for embedding in the request body
        val prompt = """
            Extract UPI payment details from this receipt screenshot. Reply with ONLY a JSON object, no markdown:
            {
              "amount": <the rupee amount paid — return as a plain number like 434, not "₹434">,
              "merchantName": "<full recipient name from text — ignore the 2-letter coloured avatar/circle, use the full name like HARISHKUMAR K>",
              "upiRef": "<UTR number if present (prefer UTR: digits only), else Transaction ID>",
              "dateText": "<full date and time as shown, e.g. '02:46 pm on 03 Jul 2026'>",
              "sourceBankHint": "<payer bank name, e.g. Jupiter, HDFC Bank, SBI>",
              "paymentApp": "<one of: PHONEPE, GPAY, PAYTM, BHIM, OTHER>"
            }
            Omit any field you cannot determine.
        """.trimIndent()
        return JSONObject.quote(prompt)
    }

    private fun decodeReceipt(json: JSONObject): UpiParsedReceipt? {
        // Models often return "₹434" or "Rs.434" despite being asked for digits-only.
        // Strip any non-numeric prefix/suffix and extract the first decimal number.
        val amountRaw = json.opt("amount")?.toString().orEmpty()
        val amount = Regex("""\d+(?:\.\d{1,2})?""")
            .find(amountRaw.replace(",", ""))
            ?.value?.toDoubleOrNull()
        // Still require a positive amount — a zero means the model missed it entirely
        if (amount == null || amount <= 0) return null

        val dateText = json.optString("dateText").takeIf { it.isNotBlank() }
        val paymentApp = json.optString("paymentApp")
            .takeIf { it.isNotBlank() }
            ?.uppercase()
            ?: "OTHER"

        return UpiParsedReceipt(
            amount = amount,
            merchantName = json.optString("merchantName").takeIf { it.isNotBlank() },
            dateTimeMillis = dateText?.let(::parseDateText),
            upiRef = json.optString("upiRef").takeIf { it.isNotBlank() },
            sourceBankHint = json.optString("sourceBankHint").takeIf { it.isNotBlank() },
            paymentApp = paymentApp,
        )
    }

    private fun parseDateText(text: String): Long? {
        val raw = text.trim()

        // Extract 12-hour time if present: "02:46 pm"
        val time12 = Regex("""(\d{1,2}):(\d{2})\s*(am|pm)""", RegexOption.IGNORE_CASE).find(raw)
        var hourOffset = 0L
        if (time12 != null) {
            var h = time12.groupValues[1].toInt()
            val m = time12.groupValues[2].toInt()
            if (time12.groupValues[3].lowercase() == "pm" && h != 12) h += 12
            if (time12.groupValues[3].lowercase() == "am" && h == 12) h = 0
            hourOffset = h * 3_600_000L + m * 60_000L
        }

        // Strip PhonePe datetime prefix: "HH:mm am/pm on " → keep the date portion only
        val dateStr = raw.replace(
            Regex("""^\d{1,2}:\d{2}\s*(am|pm)\s+on\s+""", RegexOption.IGNORE_CASE), "",
        ).trim()

        val formats = listOf("d MMM yyyy", "dd MMM yyyy", "MMM d yyyy", "yyyy-MM-dd", "d/M/yyyy")
        for (fmt in formats) {
            runCatching {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.isLenient = false
                sdf.parse(dateStr)?.time
            }.getOrNull()?.let { return it + hourOffset }
        }
        return null
    }

    private fun post(apiKey: String, body: String): String {
        val conn = URL("https://integrate.api.nvidia.com/v1/chat/completions")
            .openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
            conn.disconnect()
            throw IllegalStateException("HTTP $code: $err")
        }
        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        return response
    }

    private fun extractContent(responseJson: String): String? = runCatching {
        JSONObject(responseJson)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }.getOrNull()

    private fun extractJsonObject(text: String): JSONObject? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val scale = 1024f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun runOcr(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        recognizer.close()
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        recognizer.close()
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
}
