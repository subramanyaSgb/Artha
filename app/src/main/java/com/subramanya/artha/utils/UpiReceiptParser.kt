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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 * Both paths run in parallel and their results are merged field-by-field:
 *  - NIM vision (z-ai/glm-5.2): handles any UPI app layout, requires API key + internet.
 *  - ML Kit OCR + PhonePeReceiptParser: on-device, works offline, PhonePe only.
 *
 * Merging means a field that NIM misses can still come from ML Kit and vice-versa.
 */
class UpiReceiptParser(
    private val keyProvider: (suspend () -> String)? = null,
) {

    suspend fun parse(context: Context, uri: Uri): UpiParsedReceipt? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        val key = keyProvider?.invoke()?.takeIf { it.isNotBlank() }

        // Run NIM and ML Kit in parallel — total wait = slowest of the two, not the sum.
        return coroutineScope {
            val nimDeferred = async(Dispatchers.IO) {
                if (key != null) runCatching { parseWithNim(key, bitmap) }.getOrNull() else null
            }
            val mlDeferred = async {
                runCatching {
                    val text = runOcr(bitmap)
                    PhonePeReceiptParser.parse(text)
                }.getOrNull()
            }
            merge(nimDeferred.await(), mlDeferred.await())
        }
    }

    /** Prefer NIM for each field; fall back to ML Kit where NIM returned null. */
    private fun merge(nim: UpiParsedReceipt?, ml: UpiParsedReceipt?): UpiParsedReceipt? {
        if (nim == null && ml == null) return null
        if (nim == null) return ml
        if (ml == null) return nim
        return UpiParsedReceipt(
            amount = nim.amount ?: ml.amount,
            merchantName = nim.merchantName ?: ml.merchantName,
            dateTimeMillis = nim.dateTimeMillis ?: ml.dateTimeMillis,
            upiRef = nim.upiRef ?: ml.upiRef,
            sourceBankHint = nim.sourceBankHint ?: ml.sourceBankHint,
            paymentApp = if (nim.paymentApp != "OTHER") nim.paymentApp else ml.paymentApp,
        )
    }

    private suspend fun parseWithNim(key: String, bitmap: Bitmap): UpiParsedReceipt? {
        val b64 = bitmapToBase64(bitmap)
        // minimaxai/minimax-m3 — multimodal, extracts vision fields more reliably than
        // glm-5.2 which was dropping amount/merchant. Same OpenAI-compatible endpoint.
        val body = """
            {
              "model": "minimaxai/minimax-m3",
              "messages": [{
                "role": "user",
                "content": [
                  {"type": "text", "text": ${nimPrompt()}},
                  {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,$b64"}}
                ]
              }],
              "temperature": 0.1,
              "max_tokens": 1024,
              "stream": false
            }
        """.trimIndent()

        val raw = post(key, body)
        val content = extractContent(raw) ?: return null
        val json = extractJsonObject(content) ?: return null
        return decodeReceipt(json)
    }

    private fun nimPrompt(): String {
        val prompt = """
            Extract UPI payment details from this receipt screenshot. Reply with ONLY a JSON object, no markdown fences.
            Return ONLY these keys (omit any you cannot determine):
            amount       - the rupee amount as a plain number, e.g. 434
            merchantName - the full recipient name from text, NOT the 2-letter avatar initials (e.g. HARISHKUMAR K not HK)
            upiRef       - the UTR number (digits only, prefer UTR over Transaction ID)
            dateText     - full date+time as shown, e.g. 02:46 pm on 03 Jul 2026
            sourceBankHint - payer bank name, e.g. Jupiter, HDFC Bank, SBI
            paymentApp   - one of PHONEPE GPAY PAYTM BHIM OTHER
        """.trimIndent()
        return JSONObject.quote(prompt)
    }

    private fun decodeReceipt(json: JSONObject): UpiParsedReceipt? {
        // Models often return "₹434" or "Rs.434" despite being asked for plain numbers.
        // Use a digit-extraction regex so any currency prefix is stripped automatically.
        val amountRaw = str(json, "amount").orEmpty()
        val amount = Regex("""\d+(?:\.\d{1,2})?""")
            .find(amountRaw.replace(",", ""))
            ?.value?.toDoubleOrNull()

        val merchantName = str(json, "merchantName")
        val dateText = str(json, "dateText")
        // Strip any non-digit chars from upiRef — model sometimes returns "UTR: 540548535287"
        val upiRef = str(json, "upiRef")?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }
        val sourceBankHint = str(json, "sourceBankHint")
        val paymentApp = str(json, "paymentApp")?.uppercase() ?: "OTHER"

        // Return null only if NIM gave us absolutely nothing — merge() will fill gaps from ML Kit
        val hasData = amount != null || merchantName != null || dateText != null ||
            upiRef != null || sourceBankHint != null
        if (!hasData) return null

        return UpiParsedReceipt(
            amount = amount,
            merchantName = merchantName,
            dateTimeMillis = dateText?.let(::parseDateText),
            upiRef = upiRef,
            sourceBankHint = sourceBankHint,
            paymentApp = paymentApp,
        )
    }

    /**
     * Reads a string field, treating absent, JSON-null, AND the literal strings
     * "null"/"n/a"/"none"/"-" as "not present".
     *
     * Why this matters: `org.json`'s [JSONObject.optString] returns the 4-char string
     * "null" (not empty!) when the value is `JSONObject.NULL`. The model returns JSON
     * null for fields it can't read, so without this guard the UI showed literal
     * "null"/"NULL" for merchant, bank and payment-app.
     */
    private fun str(json: JSONObject, key: String): String? {
        if (json.isNull(key)) return null
        val v = json.optString(key).trim()
        if (v.isEmpty()) return null
        return if (v.lowercase() in ABSENT_TOKENS) null else v
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
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
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

    private companion object {
        // Values a model returns to mean "I couldn't find this" — treated as absent.
        val ABSENT_TOKENS = setOf("null", "n/a", "na", "none", "-", "unknown", "not found")
    }
}
