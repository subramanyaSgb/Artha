package com.subramanya.artha.domain.model

/**
 * A parsed bank SMS awaiting review, in domain terms. See
 * [com.subramanya.artha.data.entity.PendingSmsEntity] for the storage/why.
 */
data class PendingSms(
    val id: String,
    val receivedAt: Long,
    val sender: String,
    val rawBody: String,
    val amount: Double?,
    /** true = money left the account (→ EXPENSE); false = money came in (→ INCOME). */
    val isDebit: Boolean,
    val merchant: String?,
    val accountHint: String?,
    val refNo: String?,
    val occurredAt: Long?,
    val matchedAccountId: String?,
    val suggestedCategoryId: String?,
    val parseSource: String,
) {
    /** The date to stamp on the confirmed transaction. */
    val effectiveDate: Long get() = occurredAt ?: receivedAt
}
