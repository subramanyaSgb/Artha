package com.subramanya.artha.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Everything the AI reads off a shared payment receipt. All fields optional — anything the
 * model can't determine comes back null and the review screen leaves it blank.
 *
 * `paymentAppHint`, `bankHint`, `categoryHint` are raw strings from the model; the ViewModel
 * resolves them against the user's catalogues (payment apps / accounts / categories).
 */
data class ReceiptData(
    val amount: Double?,
    val dateTimeMillis: Long?,
    val merchant: String?,
    val description: String?,
    val paymentAppHint: String?,
    val bankHint: String?,
    val categoryHint: String?,
    val upiRef: String?,
)

/**
 * Parses a shared payment-app receipt image into [ReceiptData] using NVIDIA NIM vision
 * (nemotron-omni) — a single call returning one JSON object with every field.
 *
 * Pure-AI: there is no ML Kit / regex fallback. An internet connection + a configured key
 * are required; without them [parse] returns null and the screen offers manual entry.
 */
class UpiReceiptParser(
    private val keyProvider: (suspend () -> String)? = null,
) {

    suspend fun parse(context: Context, uri: Uri): ReceiptData? {
        val key = keyProvider?.invoke()?.takeIf { it.isNotBlank() } ?: return null
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        // Retry on TRANSIENT failures — this is why receipt reading was flaky ("works sometimes":
        // a single NIM rate-limit / 5xx / timeout, or an occasional non-JSON reply, used to fail
        // outright). Each attempt re-rolls the model call. A hard auth error (401/403) is not
        // transient, so we stop immediately. Exceptions from the final attempt propagate so the
        // screen shows the real reason instead of a blanket "couldn't read".
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { parseWithNim(key, bitmap) }
            result.getOrNull()?.let { return it }
            val error = result.exceptionOrNull()
            if (error != null) {
                lastError = error
                if (isAuthError(error)) throw error // not transient — don't waste retries
            }
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        lastError?.let { throw it }
        return null
    }

    private fun isAuthError(error: Throwable): Boolean {
        val msg = error.message.orEmpty()
        return "HTTP 401" in msg || "HTTP 403" in msg
    }

    private suspend fun parseWithNim(key: String, bitmap: Bitmap): ReceiptData? {
        val b64 = bitmapToBase64(bitmap)
        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply { put("type", "text"); put("text", PROMPT) })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply { put("url", "data:image/jpeg;base64,$b64") })
                        })
                    })
                })
            })
            put("temperature", 0.2)
            put("max_tokens", 1024)
            // Nemotron is a reasoning model: with thinking ON, its reasoning_content balloons
            // and eats the whole token budget on a detail-rich receipt BEFORE it emits the JSON
            // (content comes back empty → "could not read"). Disable thinking for extraction —
            // verified to return clean JSON in ~1.3s. Do the same anywhere we use this model.
            put("chat_template_kwargs", JSONObject().apply { put("enable_thinking", false) })
            put("stream", false)
        }.toString()

        val raw = post(key, body)
        val content = extractContent(raw) ?: return null
        val json = extractJsonObject(content) ?: return null
        return decode(json)
    }

    private fun decode(json: JSONObject): ReceiptData? {
        val amount = Regex("""\d+(?:\.\d{1,2})?""")
            .find(str(json, "amount").orEmpty().replace(",", ""))
            ?.value?.toDoubleOrNull()

        val dateTime = parseDateTime(str(json, "date"), str(json, "time"))

        val data = ReceiptData(
            amount = amount,
            dateTimeMillis = dateTime,
            merchant = str(json, "merchant"),
            description = str(json, "description"),
            paymentAppHint = str(json, "paymentApp"),
            bankHint = str(json, "bank"),
            categoryHint = str(json, "category"),
            upiRef = str(json, "upiRef")?.filter { it.isDigit() }?.takeIf { it.isNotBlank() },
        )
        // Nothing usable → treat as a failed parse so the screen shows the manual-entry path.
        val hasAnything = listOf(
            data.amount, data.dateTimeMillis, data.merchant, data.description,
            data.paymentAppHint, data.bankHint, data.upiRef,
        ).any { it != null }
        return if (hasAnything) data else null
    }

    /**
     * Combines the model's date + time strings into one epoch-millis value. Date is parsed
     * from integer/named components (NOT SimpleDateFormat's greedy `yyyy`, which turns a
     * 2-digit year into year 0026 — see the date-parse gotcha), then the 12-hour time is
     * added as an offset. Returns null if the date can't be read.
     */
    private fun parseDateTime(dateText: String?, timeText: String?): Long? {
        val dateMillis = dateText?.let(::parseDate) ?: return null
        val timeOffset = timeText?.let(::parseTimeOffset) ?: 0L
        return dateMillis + timeOffset
    }

    private fun parseDate(text: String): Long? {
        val raw = text.trim()
        // Named month: "03 Jul 2026", "3-Jul-26"
        Regex("""(\d{1,2})[-/ ]([A-Za-z]{3})[-/ ](\d{2,4})""").find(raw)?.let { m ->
            MONTHS[m.groupValues[2].lowercase()]?.let { month ->
                return buildDate(normalizeYear(m.groupValues[3]), month, m.groupValues[1].toInt())
            }
        }
        // ISO: "2026-07-03"
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""").find(raw)?.let { m ->
            return buildDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
        // Numeric d-m-y: "03/07/2026", "03-07-26"
        Regex("""(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})""").find(raw)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            if (month in 1..12 && day in 1..31) return buildDate(normalizeYear(m.groupValues[3]), month, day)
        }
        return null
    }

    private fun parseTimeOffset(text: String): Long {
        val m = Regex("""(\d{1,2}):(\d{2})\s*(am|pm)?""", RegexOption.IGNORE_CASE).find(text) ?: return 0L
        var h = m.groupValues[1].toIntOrNull() ?: return 0L
        val min = m.groupValues[2].toIntOrNull() ?: 0
        when (m.groupValues[3].lowercase()) {
            "pm" -> if (h != 12) h += 12
            "am" -> if (h == 12) h = 0
        }
        return h * 3_600_000L + min * 60_000L
    }

    private fun normalizeYear(token: String): Int {
        val n = token.toIntOrNull() ?: return 0
        return if (n < 100) 2000 + n else n
    }

    private fun buildDate(year: Int, month: Int, day: Int): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            .parse("%04d-%02d-%02d".format(year, month, day))?.time
    }.getOrNull()

    /** Absent / JSON-null / literal "null" all become Kotlin null (org.json optString quirk). */
    private fun str(json: JSONObject, key: String): String? {
        if (json.isNull(key)) return null
        val v = json.optString(key).trim()
        if (v.isEmpty()) return null
        return if (v.lowercase() in ABSENT_TOKENS) null else v
    }

    private fun post(apiKey: String, body: String): String {
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
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

    private companion object {
        const val ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions"
        const val MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning"

        // Retry transient failures (rate-limit / 5xx / timeout / occasional non-JSON reply).
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 700L

        val MONTHS = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        )
        val ABSENT_TOKENS = setOf("null", "n/a", "na", "none", "-", "unknown", "not found")

        val PROMPT = """
            Extract the payment details from this receipt/payment screenshot (any UPI or banking app).
            Reply with ONLY a JSON object, no markdown fences. Use these keys, omit any you cannot read:
            {
              "amount": <the rupee amount paid, plain number like 434>,
              "date": "<transaction date, e.g. 03 Jul 2026>",
              "time": "<transaction time, e.g. 02:46 pm>",
              "merchant": "<full recipient/payee name, ignore 2-letter avatar initials>",
              "description": "<short purpose if visible, else omit>",
              "paymentApp": "<one of PHONEPE GPAY PAYTM CRED BHIM AMAZONPAY OTHER>",
              "bank": "<payer bank/account name, e.g. Jupiter, HDFC Bank>",
              "category": "<1-2 word spending category, e.g. Food, Shopping, Bills>",
              "upiRef": "<UTR or UPI reference number, digits only>"
            }
        """.trimIndent()
    }
}
