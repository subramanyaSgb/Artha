package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bank-SMS-derived transaction awaiting the user's review.
 *
 * SMS parsing is noisy (promos, OTPs, partial info), so parsed rows land here as a
 * PENDING queue rather than going straight into `transactions`. The user confirms
 * (→ a real [TransactionEntity] is created and this row deleted), edits, or dismisses
 * (→ row deleted). Nothing here affects balances until confirmed.
 *
 * The raw SMS body is retained so the review screen can show the source, and so a
 * later re-parse (better regex / model) is possible.
 */
@Entity(
    tableName = "pending_sms",
    indices = [Index(value = ["received_at"])],
)
data class PendingSmsEntity(
    @PrimaryKey
    val id: String,
    /** When the SMS arrived (epoch millis) — used for ordering the queue. */
    @ColumnInfo(name = "received_at")
    val receivedAt: Long,
    /** The SMS sender id, e.g. "AD-HDFCBK" or "JUSPAY". */
    val sender: String,
    /** Full SMS text, kept for display + possible re-parse. */
    @ColumnInfo(name = "raw_body")
    val rawBody: String,
    /** Parsed amount in INR, or null if the parser couldn't find one. */
    val amount: Double?,
    /** "DEBIT" (→ EXPENSE on confirm) or "CREDIT" (→ INCOME). */
    val direction: String,
    /** Merchant / payee text, if the parser found one. */
    val merchant: String?,
    /** Last digits of the affected account, e.g. "1234" from "A/c XX1234". */
    @ColumnInfo(name = "account_hint")
    val accountHint: String?,
    /** UPI ref / transaction id, used both for display and duplicate detection. */
    @ColumnInfo(name = "ref_no")
    val refNo: String?,
    /** Transaction date parsed from the SMS body, if present (else null → use receivedAt). */
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long?,
    /** Best-guess account id (matched via [accountHint]); user can change on confirm. */
    @ColumnInfo(name = "matched_account_id")
    val matchedAccountId: String?,
    /** Auto-suggested category id (fuzzy match on merchant); user can change. */
    @ColumnInfo(name = "suggested_category_id")
    val suggestedCategoryId: String?,
    /** "REGEX" or "NIM" — which path produced this row (debugging / analytics). */
    @ColumnInfo(name = "parse_source")
    val parseSource: String,
)
