package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType

/**
 * Transactions store BOTH the directional `type` and the affected source/destination.
 *
 * Convention (kept consistent across the data layer):
 *   - source      = the affected account/card/cash on the user's side
 *   - destination = second affected side, only meaningful for TRANSFER / CARD_PAYMENT
 *                   (and INVESTMENT_BUY/SELL in later phases)
 *   - The `type` determines whether money flows IN or OUT of source/destination.
 *
 * No foreign keys to accounts/cards because `sourceId` is polymorphic across SourceKind.
 * Indexes are added for common filters (date, category, sourceId, destinationId).
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category_id"]),
        Index(value = ["sub_category_id"]),
        Index(value = ["source_id"]),
        Index(value = ["destination_id"]),
        Index(value = ["type"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String,
    val date: Long,
    val description: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    @ColumnInfo(name = "sub_category_id")
    val subCategoryId: String?,
    @ColumnInfo(name = "source_type")
    val sourceType: SourceKind,
    @ColumnInfo(name = "source_id")
    val sourceId: String?,
    @ColumnInfo(name = "destination_type")
    val destinationType: SourceKind?,
    @ColumnInfo(name = "destination_id")
    val destinationId: String?,
    @ColumnInfo(name = "payment_app")
    val paymentApp: PaymentApp,
    val place: String?,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "receipt_uri")
    val receiptUri: String?,
    val notes: String?,
    @ColumnInfo(name = "tax_section")
    val taxSection: String?,
    @ColumnInfo(name = "recurring_rule_id")
    val recurringRuleId: String?,
    @ColumnInfo(name = "is_split")
    val isSplit: Boolean,
    @ColumnInfo(name = "split_group_id")
    val splitGroupId: String?,
    val source: TransactionSource,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    /** Set by a rule's `ExcludeFromExpenseTotal` action — the monthly expense aggregator skips
     *  these (e.g. a credit-card bill the user logged as an expense). Default false. */
    @ColumnInfo(name = "excluded_from_expense_total", defaultValue = "0")
    val excludedFromExpenseTotal: Boolean = false,
)
