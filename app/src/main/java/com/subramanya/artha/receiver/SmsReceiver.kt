package com.subramanya.artha.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.utils.sms.BankSmsParser
import com.subramanya.artha.utils.sms.SmsIngestor
import com.subramanya.artha.worker.SmsNimParseWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS and, when SMS auto-import is enabled, turns bank transaction
 * messages into review-queue rows.
 *
 * Flow per message:
 *  1. Gate with [BankSmsParser.isTransactional] — OTPs / promos / balance alerts are dropped.
 *  2. Regex parse. If it yields an amount, queue immediately (offline, free).
 *  3. If it's transactional but regex couldn't extract the amount, hand off to
 *     [SmsNimParseWorker] (the AI fallback) so we never block the receiver on the network.
 *
 * Uses [goAsync] so the short DB writes complete after onReceive returns; the network
 * fallback is deliberately offloaded to WorkManager, never run inline here.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Concatenate multipart bodies per sender, preserving order.
        val bySender = LinkedHashMap<String, StringBuilder>()
        val tsBySender = HashMap<String, Long>()
        for (m in messages) {
            val addr = m.originatingAddress ?: "unknown"
            bySender.getOrPut(addr) { StringBuilder() }.append(m.messageBody ?: "")
            tsBySender[addr] = m.timestampMillis
        }
        if (bySender.isEmpty()) return

        val app = context.applicationContext as ArthaApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!app.settingsPreferences.smsAutoImportEnabled.first()) return@launch
                for ((sender, sb) in bySender) {
                    val body = sb.toString()
                    val receivedAt = tsBySender[sender] ?: System.currentTimeMillis()
                    val parsed = BankSmsParser.parse(body, receivedAt) ?: continue // not transactional
                    if (parsed.amount != null) {
                        SmsIngestor.ingest(app, sender, body, receivedAt, parsed, parseSource = "REGEX")
                    } else {
                        // Transactional but regex missed the amount — let the AI try.
                        WorkManager.getInstance(app).enqueue(
                            OneTimeWorkRequestBuilder<SmsNimParseWorker>()
                                .setInputData(
                                    workDataOf(
                                        SmsNimParseWorker.KEY_SENDER to sender,
                                        SmsNimParseWorker.KEY_BODY to body,
                                        SmsNimParseWorker.KEY_RECEIVED_AT to receivedAt,
                                    ),
                                )
                                .build(),
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
