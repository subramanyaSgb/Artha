package com.subramanya.artha.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
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
    /** true = money received (income/credit), false = money paid (expense/debit), null = unknown. */
    val isCredit: Boolean?,
)

/**
 * Parses a shared payment-app receipt image into [ReceiptData] using AI vision.
 *
 * Provider fallback chain (each tried in order; first valid result wins):
 *  1. Groq `qwen/qwen3.6-27b` — fastest (~0.3s), free. It's a THINKING model, so we send
 *     `reasoning_effort: "none"` or it burns the token budget reasoning and returns nothing.
 *  2. RoutesMe (aggregator, `Kimi-k3`) — free, fast; occasionally returns `all_keys_failed`.
 *  3. NIM `llama-3.1-nemotron-nano-vl-8b-v1` — OCR-specialised, reliable.
 *  4. OpenRouter `nvidia/nemotron-nano-12b-v2-vl:free` — last resort (free tier queues, slow).
 * Auth errors (401/403) on a provider skip to the next; they don't abort the whole chain
 * (one bad key shouldn't kill a working fallback).
 */
class UpiReceiptParser(
    private val nimKeyProvider: (suspend () -> String)? = null,
    private val openRouterKeyProvider: (suspend () -> String)? = null,
    private val groqKeyProvider: (suspend () -> String)? = null,
    private val routesMeKeyProvider: (suspend () -> String)? = null,
    // The app owner's name (from Settings). Anchors the model so it never returns the owner as
    // the merchant and reads DEBIT/CREDIT from the owner's side — a UPI screenshot shows both
    // parties, and without this anchor the model sometimes picks the owner or flips direction.
    private val userNameProvider: (suspend () -> String)? = null,
) {

    suspend fun parse(context: Context, uri: Uri): ReceiptData? {
        val userName = userNameProvider?.invoke()?.trim().orEmpty()
        val groqKey = groqKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val routesMeKey = routesMeKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val nimKey = nimKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val orKey = openRouterKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        if (groqKey == null && routesMeKey == null && nimKey == null && orKey == null) return null

        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        val b64 = withContext(Dispatchers.IO) { bitmapToBase64(bitmap) }

        // Ordered fallback chain: (endpoint, model, key, timeout, reasoningEffortNone, extraHeaders).
        // Nulls (missing key) are skipped. First provider that returns a non-null result wins.
        val providers = listOfNotNull(
            groqKey?.let { Provider(GROQ_ENDPOINT, GROQ_MODEL, it, 20_000, reasoningEffortNone = true) },
            routesMeKey?.let { Provider(ROUTESME_ENDPOINT, ROUTESME_MODEL, it, 20_000) },
            nimKey?.let { Provider(NIM_ENDPOINT, NIM_MODEL, it, 15_000) },
            orKey?.let {
                Provider(OR_ENDPOINT, OR_MODEL, it, 20_000, extraHeaders = mapOf(
                    "HTTP-Referer" to "https://github.com/subramanyaSgb/Artha",
                ))
            },
        )

        for (p in providers) {
            val result = runCatching {
                callProvider(p.endpoint, p.model, p.key, b64, p.timeoutMs, userName, p.reasoningEffortNone, p.extraHeaders)
            }
            result.getOrNull()?.let { return it }
            // Any failure (auth, timeout, 5xx, empty) → try the next provider in the chain.
        }
        return null
    }

    private data class Provider(
        val endpoint: String,
        val model: String,
        val key: String,
        val timeoutMs: Int,
        val reasoningEffortNone: Boolean = false,
        val extraHeaders: Map<String, String> = emptyMap(),
    )

    private fun callProvider(
        endpoint: String,
        model: String,
        key: String,
        b64: String,
        timeoutMs: Int,
        userName: String,
        reasoningEffortNone: Boolean = false,
        extraHeaders: Map<String, String> = emptyMap(),
    ): ReceiptData? {
        val body = JSONObject().apply {
            put("model", model)
            // Thinking models (Groq's qwen3.6) otherwise spend the whole token budget on a
            // <think> block and return no answer — see the nemotron-disable-thinking gotcha.
            if (reasoningEffortNone) put("reasoning_effort", "none")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply { put("type", "text"); put("text", buildPrompt(userName)) })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply { put("url", "data:image/jpeg;base64,$b64") })
                        })
                    })
                })
            })
            put("temperature", 0.2)
            put("max_tokens", 512)
            put("stream", false)
        }.toString()

        val raw = post(endpoint, key, body, timeoutMs, extraHeaders)
        val content = extractContent(raw) ?: return null
        val json = extractJsonObject(content) ?: return null
        return decode(json, userName)
    }

    /** Test seam: decode a JSON string directly (no network), applying the owner-name filter. */
    internal fun decodeForTest(jsonString: String, userName: String): ReceiptData? =
        runCatching { JSONObject(jsonString) }.getOrNull()?.let { decode(it, userName) }

    private fun decode(json: JSONObject, userName: String): ReceiptData? {
        val amount = Regex("""\d+(?:\.\d{1,2})?""")
            .find(str(json, "amount").orEmpty().replace(",", ""))
            ?.value?.toDoubleOrNull()

        val dateTime = parseDateTime(str(json, "date"), str(json, "time"))

        val data = ReceiptData(
            amount = amount,
            dateTimeMillis = dateTime,
            // Safety net: if the model still returned the owner as the merchant, drop it —
            // the owner is never the counterparty. (The prompt already tells it not to.)
            merchant = str(json, "merchant")?.takeUnless { isOwnerName(it, userName) },
            description = str(json, "description"),
            paymentAppHint = str(json, "paymentApp"),
            bankHint = str(json, "bank"),
            categoryHint = str(json, "category"),
            upiRef = str(json, "upiRef")?.filter { it.isDigit() }?.takeIf { it.isNotBlank() },
            isCredit = when (str(json, "direction")?.uppercase()) {
                "CREDIT" -> true
                "DEBIT" -> false
                else -> null
            },
        )
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
        Regex("""(\d{1,2})[-/ ]([A-Za-z]{3})[-/ ](\d{2,4})""").find(raw)?.let { m ->
            MONTHS[m.groupValues[2].lowercase()]?.let { month ->
                return buildDate(normalizeYear(m.groupValues[3]), month, m.groupValues[1].toInt())
            }
        }
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""").find(raw)?.let { m ->
            return buildDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
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

    private fun str(json: JSONObject, key: String): String? {
        if (json.isNull(key)) return null
        val v = json.optString(key).trim()
        if (v.isEmpty()) return null
        return if (v.lowercase() in ABSENT_TOKENS) null else v
    }

    private fun post(
        endpoint: String,
        apiKey: String,
        body: String,
        timeoutMs: Int,
        extraHeaders: Map<String, String>,
    ): String {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
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

    /**
     * True if [candidate] is (or closely overlaps) the app owner's [userName] — used to reject
     * the owner being returned as the merchant. Compares case-insensitively and also treats a
     * significant word-overlap as a match (handles "Subramanya G B" vs "Subramanya Gopal Bellary").
     */
    internal fun isOwnerName(candidate: String, userName: String): Boolean {
        val owner = userName.trim()
        if (owner.isBlank()) return false
        val a = candidate.trim().lowercase()
        val b = owner.lowercase()
        if (a == b || a.contains(b) || b.contains(a)) return true
        val aWords = a.split(Regex("\\s+")).filter { it.length >= 3 }.toSet()
        val bWords = b.split(Regex("\\s+")).filter { it.length >= 3 }.toSet()
        if (aWords.isEmpty() || bWords.isEmpty()) return false
        val shared = aWords.count { it in bWords }
        // ≥2 shared name words, or the candidate's words are entirely a subset of the owner's.
        return shared >= 2 || aWords.all { it in bWords }
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
        const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        const val GROQ_MODEL = "qwen/qwen3.6-27b"

        const val ROUTESME_ENDPOINT = "https://routesme.online/v1/chat/completions"
        const val ROUTESME_MODEL = "Kimi-k3"

        const val NIM_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions"
        const val NIM_MODEL = "nvidia/llama-3.1-nemotron-nano-vl-8b-v1"

        const val OR_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        const val OR_MODEL = "nvidia/nemotron-nano-12b-v2-vl:free"

        val MONTHS = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        )
        val ABSENT_TOKENS = setOf("null", "n/a", "na", "none", "-", "unknown", "not found")

    }

    /**
     * Builds the extraction prompt. When [userName] is known, it's injected as the account
     * owner so the model can tell the two on-screen parties apart — a UPI screenshot shows both
     * the owner and the counterparty, and without this anchor the model sometimes returns the
     * owner as the merchant or flips DEBIT/CREDIT.
     */
    internal fun buildPrompt(userName: String): String {
        val ownerLine = userName.trim().takeIf { it.isNotBlank() }?.let {
            """
            The account owner (the app user) is "$it". This name may appear on the screenshot as
            a header/avatar — it is NEVER the merchant. The "merchant" is always the OTHER party.
            direction is from the owner's side: DEBIT if "$it" PAID/SENT the money, CREDIT if "$it"
            RECEIVED it.
            """.trimIndent()
        } ?: """
            direction: DEBIT if money was PAID/SENT/DEBITED by the account owner, CREDIT if money
            was RECEIVED/CREDITED (look for "received", "credited", "paid to you", green +amount).
            """.trimIndent()

        return """
            Extract the payment details from this receipt/payment screenshot (any UPI or banking app).
            Reply with ONLY a JSON object, no markdown fences. Use these keys, omit any you cannot read.
            For date/time use EXACTLY these formats: date as YYYY-MM-DD, time as HH:MM in 24-hour clock.
            $ownerLine
            {
              "amount": <the rupee amount, plain number like 434>,
              "date": "<YYYY-MM-DD, e.g. 2026-07-03>",
              "time": "<HH:MM 24-hour, e.g. 14:46>",
              "direction": "<DEBIT or CREDIT>",
              "merchant": "<full other-party name (NOT the account owner), ignore 2-letter avatar initials>",
              "description": "<short purpose if visible, else omit>",
              "paymentApp": "<one of PHONEPE GPAY PAYTM CRED BHIM AMAZONPAY OTHER>",
              "bank": "<payer bank/account name, e.g. Jupiter, HDFC Bank>",
              "category": "<1-2 word spending category, e.g. Food, Shopping, Bills>",
              "upiRef": "<UTR or UPI reference number, digits only>"
            }
        """.trimIndent()
    }
}
