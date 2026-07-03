package com.subramanya.artha.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal text-only NVIDIA NIM chat helper, shared by tasks that only need a single
 * text completion (e.g. the SMS parse fallback). Vision tasks build their own request
 * bodies; this is deliberately tiny — one prompt in, model `content` string out.
 */
object NimTextClient {

    private const val ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions"
    private const val MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning"

    /** Returns the model's `message.content`, or null on any failure. */
    suspend fun complete(apiKey: String, prompt: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        runCatching {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.2)
                // Reasoning model: reasoning_content counts toward the completion budget.
                put("max_tokens", 4096)
                put("stream", false)
            }.toString()

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
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@runCatching null
            }
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }.getOrNull()
    }
}
