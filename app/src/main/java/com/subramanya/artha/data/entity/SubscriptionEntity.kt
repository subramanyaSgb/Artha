package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.SubscriptionFrequency
import com.subramanya.artha.data.entity.enums.SubscriptionStatus

/**
 * PRD §7.15 Subscription. The "Add Subscription" flow may optionally spawn a
 * RecurringRule to auto-create monthly transactions; that's a separate row in
 * recurring_rules with its own lifecycle.
 */
@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["status"]), Index(value = ["next_due_date"])],
)
data class SubscriptionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val provider: String?,
    val amount: Double,
    val frequency: SubscriptionFrequency,
    @ColumnInfo(name = "next_due_date")
    val nextDueDate: Long,
    @ColumnInfo(name = "last_paid_date")
    val lastPaidDate: Long?,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    /** SourceKind.name string — kept loose because subscriptions may bill to either an
     *  account or a card and we don't want a foreign key to constrain that. */
    @ColumnInfo(name = "payment_method_type")
    val paymentMethodType: String?,
    @ColumnInfo(name = "payment_method_id")
    val paymentMethodId: String?,
    val status: SubscriptionStatus,
    @ColumnInfo(name = "auto_charge")
    val autoCharge: Boolean,
    @ColumnInfo(name = "logo_uri")
    val logoUri: String?,
    val color: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
