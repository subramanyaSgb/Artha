package com.subramanya.artha.utils.sms

import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.domain.model.PendingSms
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Turns a [ParsedSms] into a queued [PendingSms] row: de-duplicates by ref, best-guesses
 * the affected account (via last-4 match) and a category (fuzzy merchant match), then
 * inserts. Shared by the regex path ([SmsReceiver]) and the NIM path
 * ([com.subramanya.artha.worker.SmsNimParseWorker]) so both behave identically.
 */
object SmsIngestor {

    suspend fun ingest(
        app: ArthaApplication,
        sender: String,
        rawBody: String,
        receivedAt: Long,
        parsed: ParsedSms,
        parseSource: String,
    ) {
        // Need at least an amount to be worth queuing.
        val amount = parsed.amount ?: return

        // Duplicate guard by ref:
        //  - already queued (receiver + a re-delivered SMS), or
        //  - already a real transaction (e.g. the user shared the UPI receipt, which stored
        //    "UPI Ref: <ref>" in notes) — don't double-count the same payment.
        parsed.refNo?.let { ref ->
            if (app.pendingSmsRepository.existsByRef(ref)) return
            if (app.transactionRepository.existsByRef(ref)) return
        }

        val matchedAccountId = parsed.accountHint?.let { hint ->
            val accounts = app.accountRepository.observeAll().first()
            accounts.firstOrNull { it.accountNumberLast4 == hint }?.id
                ?: accounts.firstOrNull { it.name.contains(hint) }?.id
        }

        val suggestedCategoryId = parsed.merchant?.let { merchant ->
            val categories = app.categoryRepository.observeAll().first()
            val words = merchant.split(Regex("[\\s,./\\-_]+")).filter { it.length >= 3 }
            categories.firstOrNull { cat ->
                words.any { w ->
                    cat.name.contains(w, ignoreCase = true) || w.contains(cat.name, ignoreCase = true)
                }
            }?.id
        }

        app.pendingSmsRepository.insert(
            PendingSms(
                id = UUID.randomUUID().toString(),
                receivedAt = receivedAt,
                sender = sender,
                rawBody = rawBody,
                amount = amount,
                isDebit = parsed.isDebit,
                merchant = parsed.merchant,
                accountHint = parsed.accountHint,
                refNo = parsed.refNo,
                occurredAt = parsed.occurredAt,
                matchedAccountId = matchedAccountId,
                suggestedCategoryId = suggestedCategoryId,
                parseSource = parseSource,
            ),
        )
    }
}
