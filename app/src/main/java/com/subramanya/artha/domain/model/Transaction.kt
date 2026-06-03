package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType

/**
 * source     = the affected account/card/cash on the user's side.
 * destination = second affected account/card on the user's side (transfers, card payments).
 * The `type` determines direction (money in vs out).
 *
 * peopleIds / tagIds are populated by the repository from cross-ref tables; they are
 * empty lists when the row was loaded without those joins.
 */
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String,
    val date: Long,
    val description: String,
    val categoryId: String?,
    val subCategoryId: String?,
    val sourceType: SourceKind,
    val sourceId: String?,
    val destinationType: SourceKind?,
    val destinationId: String?,
    val paymentApp: PaymentApp,
    val place: String?,
    val latitude: Double?,
    val longitude: Double?,
    val peopleIds: List<String>,
    val tagIds: List<String>,
    val receiptUri: String?,
    val notes: String?,
    val taxSection: String?,
    val recurringRuleId: String?,
    val isSplit: Boolean,
    val splitGroupId: String?,
    val source: TransactionSource,
    val createdAt: Long,
    val updatedAt: Long,
    /** A rule's ExcludeFromExpenseTotal action marked this — the monthly aggregator skips it. */
    val excludedFromExpenseTotal: Boolean = false,
)
