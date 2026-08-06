package com.subramanya.artha.utils

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses an insurance policy document (its first pages, pre-rasterized to base64 JPEG by
 * Task 4) into [PolicyData] using AI vision. Mirrors [UpiReceiptParser] — same 4-provider
 * fallback chain, same post/extract/decode helpers (duplicated rather than abstracted; two
 * callers don't justify a shared base).
 *
 * Provider fallback chain (each tried in order; first valid decoded result wins):
 *  1. Groq `qwen/qwen3.6-27b` — fast, free. THINKING model → `reasoning_effort: "none"` or
 *     it burns the whole token budget on a <think> block and returns nothing.
 *  2. RoutesMe (`Kimi-k3`) — free aggregator.
 *  3. NIM `llama-3.1-nemotron-nano-vl-8b-v1` — OCR-specialised.
 *  4. OpenRouter `nvidia/nemotron-nano-12b-v2-vl:free` — last resort.
 */
class PolicyDocParser(
    private val groqKeyProvider: (suspend () -> String)? = null,
    private val routesMeKeyProvider: (suspend () -> String)? = null,
    private val nimKeyProvider: (suspend () -> String)? = null,
    private val openRouterKeyProvider: (suspend () -> String)? = null,
    // Extra Groq keys (separate accounts) tried right after the primary Groq. A multi-page
    // policy PDF (~2.5K vision tokens/page) can hit one key's 8K-tokens/min cap → 429; each
    // backup key has its own quota, so the fast Groq path survives the spike.
    private val groqBackupKeysProvider: (suspend () -> List<String>)? = null,
) {

    /** All page images go in one request (one image_url entry each) + the policy prompt. */
    suspend fun parse(pageImagesB64: List<String>): PolicyData? {
        if (pageImagesB64.isEmpty()) return null
        val groqKey = groqKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val groqBackupKeys = groqBackupKeysProvider?.invoke()?.filter { it.isNotBlank() }.orEmpty()
        val routesMeKey = routesMeKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val nimKey = nimKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        val orKey = openRouterKeyProvider?.invoke()?.takeIf { it.isNotBlank() }
        if (groqKey == null && groqBackupKeys.isEmpty() && routesMeKey == null && nimKey == null && orKey == null) return null

        fun groqProvider(key: String) = Provider(GROQ_ENDPOINT, GROQ_MODEL, key, 30_000, reasoningEffortNone = true)

        val providers = buildList {
            groqKey?.let { add(groqProvider(it)) }
            groqBackupKeys.forEach { add(groqProvider(it)) }  // separate accounts → separate TPM quota
            routesMeKey?.let { add(Provider(ROUTESME_ENDPOINT, ROUTESME_MODEL, it, 30_000)) }
            nimKey?.let { add(Provider(NIM_ENDPOINT, NIM_MODEL, it, 25_000)) }
            orKey?.let {
                add(Provider(OR_ENDPOINT, OR_MODEL, it, 30_000, extraHeaders = mapOf(
                    "HTTP-Referer" to "https://github.com/subramanyaSgb/Artha",
                )))
            }
        }

        for (p in providers) {
            val result = runCatching {
                callProvider(p.endpoint, p.model, p.key, pageImagesB64, p.timeoutMs, p.reasoningEffortNone, p.extraHeaders)
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
        pageImagesB64: List<String>,
        timeoutMs: Int,
        reasoningEffortNone: Boolean = false,
        extraHeaders: Map<String, String> = emptyMap(),
    ): PolicyData? {
        val body = JSONObject().apply {
            put("model", model)
            if (reasoningEffortNone) put("reasoning_effort", "none")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply { put("type", "text"); put("text", PROMPT) })
                        pageImagesB64.forEach { b64 ->
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply { put("url", "data:image/jpeg;base64,$b64") })
                            })
                        }
                    })
                })
            })
            put("temperature", 0.2)
            put("max_tokens", 2048)
            put("stream", false)
        }.toString()

        val raw = post(endpoint, key, body, timeoutMs, extraHeaders)
        val content = extractContent(raw) ?: return null
        val json = extractJsonObject(content) ?: return null
        return decode(json)
    }

    /** Test seam: decode a JSON string directly, no network. */
    internal fun decodeForTest(jsonString: String): PolicyData? =
        runCatching { JSONObject(jsonString) }.getOrNull()?.let { decode(it) }

    internal fun decode(json: JSONObject): PolicyData? {
        val data = PolicyData(
            name = str(json, "name"),
            typeHint = str(json, "type"),
            provider = str(json, "provider"),
            policyNumber = str(json, "policyNumber"),
            sumAssured = amount(json, "sumAssured"),
            premiumAmount = amount(json, "premiumAmount"),
            premiumFrequencyHint = str(json, "premiumFrequency"),
            startDateMillis = str(json, "startDate")?.let(::parseDate),
            endDateMillis = str(json, "endDate")?.let(::parseDate),
            nextDueMillis = str(json, "nextDueDate")?.let(::parseDate),
            nominee = str(json, "nominee"),
            taxSection = str(json, "taxSection"),
            planName = str(json, "planName"),
            policyTerm = str(json, "policyTerm"),
            lifeAssured = str(json, "lifeAssured"),
            uin = str(json, "uin"),
            insurerHelpline = str(json, "insurerHelpline"),
            detailsJson = json.toString(),
        )
        val hasAnything = listOf(
            data.name, data.provider, data.policyNumber, data.sumAssured,
            data.premiumAmount, data.startDateMillis, data.planName, data.uin,
        ).any { it != null }
        return if (hasAnything) data else null
    }

    /** Rupee amount from either a JSON number or a "₹10,00,000" string → Double. */
    private fun amount(json: JSONObject, key: String): Double? =
        Regex("""\d+(?:\.\d{1,2})?""")
            .find(str(json, key).orEmpty().replace(",", ""))
            ?.value?.toDoubleOrNull()

    /**
     * Parse a date from integer/named components (NOT SimpleDateFormat's greedy `yyyy`, which
     * turns a 2-digit year into year 0026 — see the date-parse gotcha).
     */
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

        val PROMPT = """
            Read this insurance policy document (one or more page images) and extract its details.
            Reply with ONLY a JSON object, no markdown fences. Use these keys; OMIT any you cannot read.
            For all dates use EXACTLY YYYY-MM-DD. Amounts as plain numbers (no ₹, no commas).
            {
              "name": "<policy/product name>",
              "type": "<one of HEALTH VEHICLE LIFE_TERM LIFE_ENDOWMENT TRAVEL HOME OTHER>",
              "provider": "<insurer company name>",
              "policyNumber": "<policy number>",
              "sumAssured": <plain number>,
              "premiumAmount": <plain number>,
              "premiumFrequency": "<one of MONTHLY QUARTERLY HALF_YEARLY YEARLY SINGLE>",
              "startDate": "<YYYY-MM-DD>",
              "endDate": "<YYYY-MM-DD>",
              "nextDueDate": "<YYYY-MM-DD>",
              "nominee": "<nominee name>",
              "taxSection": "<e.g. 80C, 80D>",
              "planName": "<plan/variant name>",
              "policyTerm": "<e.g. 2 years>",
              "lifeAssured": "<insured person name>",
              "uin": "<UIN / product code>",
              "insurerHelpline": "<helpline phone number>",
              "members": [{"name": "", "relation": "", "age": 0}],
              "riders": [{"name": "", "premium": 0, "note": ""}],
              "coverage": [{"label": "", "value": ""}],
              "exclusions": ["<string>"],
              "contacts": {"helpline": "", "claimsEmail": "", "branch": "", "tpa": ""},
              "premiumBreakdown": {"base": 0, "riders": 0, "gst": 0, "total": 0},
              "status": "<ACTIVE / LAPSED / etc.>"
            }
        """.trimIndent()
    }
}
