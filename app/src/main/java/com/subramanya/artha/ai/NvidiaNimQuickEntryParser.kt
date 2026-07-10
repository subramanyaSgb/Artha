package com.subramanya.artha.ai

import android.graphics.Bitmap
import android.util.Base64
import com.subramanya.artha.data.db.seed.SeedPaymentApps
import com.subramanya.artha.data.entity.enums.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * NVIDIA NIM (OpenAI-compatible) implementation of [AiQuickEntryParser].
 *
 * Model: nvidia/llama-3.1-nemotron-nano-vl-8b-v1 — supports text and vision input.
 * Endpoint: https://integrate.api.nvidia.com/v1/chat/completions
 *
 * The API key is baked from local.properties → BuildConfig.NIM_API_KEY.
 */
class NvidiaNimQuickEntryParser(
    private val keyProvider: suspend () -> String,
) : AiQuickEntryParser {

    private val endpoint = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val model = "nvidia/llama-3.1-nemotron-nano-vl-8b-v1"

    override suspend fun parse(input: AiQuickEntryInput): AiQuickEntryResult {
        val key = keyProvider()
        if (key.isBlank()) return AiQuickEntryResult.NoApiKey

        return withContext(Dispatchers.IO) {
            runCatching {
                val body = buildRequestBody(input)
                val raw = post(key, body)
                val content = extractContent(raw) ?: throw IllegalStateException("Empty response from model")
                val json = extractJsonObject(content) ?: throw IllegalStateException("Model didn't return JSON: $content")
                AiQuickEntryResult.Success(decode(json, content))
            }.getOrElse { AiQuickEntryResult.Error(it.message ?: "NVIDIA NIM call failed") }
        }
    }

    override suspend fun validateKey(candidate: String): KeyValidationResult {
        if (candidate.isBlank()) return KeyValidationResult.Invalid("Key is empty")
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Reply with just: ok")
                        })
                    })
                    put("max_tokens", 8)
                    put("stream", false)
                }.toString()
                val response = post(candidate, body, timeoutMs = 10_000)
                val content = extractContent(response)
                if (content != null) KeyValidationResult.Ok
                else KeyValidationResult.Invalid("Unexpected response")
            }.getOrElse { err ->
                val msg = err.message.orEmpty()
                val isAuth = listOf("401", "403", "invalid", "unauthorized", "api key", "apikey")
                    .any { it in msg.lowercase() }
                if (isAuth) KeyValidationResult.Invalid(msg) else KeyValidationResult.NetworkError(msg)
            }
        }
    }

    private fun buildRequestBody(input: AiQuickEntryInput): String {
        val prompt = buildPrompt(input.text)
        val messageContent: Any = if (input.photo != null) {
            // Vision: content as array with text + image_url
            JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,${bitmapToBase64(input.photo)}")
                    })
                })
            }
        } else {
            prompt
        }

        return JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", messageContent)
                })
            })
            put("temperature", 0.3)
            put("max_tokens", 1024)
            put("stream", false)
        }.toString()
    }

    private fun post(apiKey: String, body: String, timeoutMs: Int = 30_000): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
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

    /** Extracts `choices[0].message.content` from an OpenAI-format response. */
    private fun extractContent(responseJson: String): String? = runCatching {
        val root = JSONObject(responseJson)
        root.getJSONArray("choices")
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

    private fun buildPrompt(userText: String): String = """
        You are a finance-app assistant for a user in India. Parse the input into a single
        JSON object with these fields (all optional — omit any you cannot determine):
          - type: one of EXPENSE, INCOME, TRANSFER
          - amount: number in INR rupees (digits only, no symbols)
          - description: short human-readable merchant or purpose
          - category: 1-2 word category (e.g. Food, Transport, Bills, Shopping, Health)
          - paymentApp: one of GPAY, PHONEPE, PAYTM, CRED, BHIM, CARD_SWIPE, CASH, OTHER
          - dateText: relative date phrase like "today", "yesterday", "3 days ago"
          - place: location or merchant name if mentioned

        Reply with ONLY the JSON object. No prose, no markdown fences, no extra keys.
        User input: ${userText.ifBlank { "(see attached image — extract transaction details)" }}
    """.trimIndent()

    private fun decode(json: JSONObject, raw: String): AiQuickEntryParsed {
        val typeStr = json.optString("type").takeIf { it.isNotBlank() }
        val amount = json.opt("amount")?.toString()?.replace(",", "")?.toDoubleOrNull()
        val description = json.optString("description").takeIf { it.isNotBlank() }
        val category = json.optString("category").takeIf { it.isNotBlank() }
        val paymentAppStr = json.optString("paymentApp").takeIf { it.isNotBlank() }
        val dateText = json.optString("dateText").takeIf { it.isNotBlank() }
        val place = json.optString("place").takeIf { it.isNotBlank() }

        val type = runCatching { typeStr?.let { TransactionType.valueOf(it.uppercase()) } }.getOrNull()
        val builtinAppIds = SeedPaymentApps.BUILTINS.mapTo(HashSet()) { it.first }
        val app = paymentAppStr?.uppercase()?.takeIf { it in builtinAppIds } ?: SeedPaymentApps.DEFAULT_ID

        // Default date to today if model didn't detect one — MEDIUM confidence (today is a safe assumption)
        val now = Clock.System.now().toEpochMilliseconds()
        val resolvedDate = dateText?.let(::resolveRelativeDate) ?: now
        val dateConfidence = if (dateText != null) Confidence.MEDIUM else Confidence.MEDIUM

        // "OTHER" is a valid fallback — show as MEDIUM so it doesn't alarm the user with red
        val appConfidence = if (paymentAppStr != null) Confidence.HIGH else Confidence.MEDIUM

        return AiQuickEntryParsed(
            type = AiField(type, if (type != null) Confidence.HIGH else Confidence.LOW),
            amount = AiField(amount, if (amount != null) Confidence.HIGH else Confidence.LOW),
            description = AiField(description, if (description != null) Confidence.MEDIUM else Confidence.LOW),
            categoryHint = AiField(category, if (category != null) Confidence.MEDIUM else Confidence.LOW),
            paymentApp = AiField(app, appConfidence),
            dateMillis = AiField(resolvedDate, dateConfidence),
            place = AiField(place, if (place != null) Confidence.MEDIUM else Confidence.LOW),
            rawModelResponse = raw,
        )
    }

    private fun resolveRelativeDate(phrase: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val daysAgo = when {
            "yesterday" in phrase.lowercase() -> 1
            "today" in phrase.lowercase() -> 0
            phrase.matches(Regex("""\d+\s*days?\s*ago""", RegexOption.IGNORE_CASE)) ->
                Regex("""\d+""").find(phrase)?.value?.toIntOrNull() ?: 0
            else -> 0
        }
        return now - daysAgo * 86_400_000L
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        // Scale down if too large — NVIDIA has a payload size limit
        val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val scale = 1024f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
