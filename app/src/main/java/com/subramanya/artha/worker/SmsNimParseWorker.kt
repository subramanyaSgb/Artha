package com.subramanya.artha.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.ai.NimTextClient
import com.subramanya.artha.utils.sms.BankSmsParser
import com.subramanya.artha.utils.sms.ParsedSms
import com.subramanya.artha.utils.sms.SmsIngestor
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/**
 * AI fallback for SMS the regex parser flagged as transactional but couldn't fully read.
 * Runs off the receiver (network call) so onReceive never risks an ANR. On success it
 * queues a [com.subramanya.artha.domain.model.PendingSms] just like the regex path.
 */
class SmsNimParseWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ArthaApplication
        // Toggle may have been turned off between enqueue and run.
        if (!app.settingsPreferences.smsAutoImportEnabled.first()) return Result.success()

        val sender = inputData.getString(KEY_SENDER) ?: return Result.success()
        val body = inputData.getString(KEY_BODY) ?: return Result.success()
        val receivedAt = inputData.getLong(KEY_RECEIVED_AT, System.currentTimeMillis())

        val key = app.nimApiKey()
        if (key.isBlank()) return Result.success() // no key → nothing to do

        val content = NimTextClient.complete(key, buildPrompt(body))
            ?: return Result.retry() // network/transient — WorkManager backs off
        val json = extractJson(content) ?: return Result.success()

        val amount = Regex("""\d+(?:\.\d{1,2})?""")
            .find(json.optString("amount").replace(",", ""))
            ?.value?.toDoubleOrNull()
            ?: return Result.success() // model couldn't find an amount either — drop it

        val direction = json.optString("direction").uppercase()
        val isDebit = when {
            direction.contains("DEBIT") -> true
            direction.contains("CREDIT") -> false
            else -> BankSmsParser.parse(body, receivedAt)?.isDebit ?: true
        }
        val parsed = ParsedSms(
            amount = amount,
            isDebit = isDebit,
            merchant = json.optString("merchant").takeIf { it.isNotBlank() && !it.equals("null", true) },
            accountHint = json.optString("accountHint").filter { it.isDigit() }.takeIf { it.isNotBlank() },
            refNo = json.optString("refNo").takeIf { it.isNotBlank() && !it.equals("null", true) },
            occurredAt = receivedAt,
        )
        SmsIngestor.ingest(app, sender, body, receivedAt, parsed, parseSource = "NIM")
        return Result.success()
    }

    private fun buildPrompt(sms: String): String = """
        Extract the bank transaction from this SMS. Reply with ONLY a JSON object, no markdown:
        {"amount": <number>, "direction": "DEBIT or CREDIT", "merchant": "<payee/merchant>", "accountHint": "<last 4 digits of account>", "refNo": "<reference/UTR number>"}
        Omit any field you cannot determine. SMS: $sms
    """.trimIndent()

    private fun extractJson(text: String): JSONObject? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
    }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_RECEIVED_AT = "received_at"
    }
}
