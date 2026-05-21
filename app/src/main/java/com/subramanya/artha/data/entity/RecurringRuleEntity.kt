package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.RecurringFrequency

/**
 * PRD §7.16 Recurring transaction rule. The `transactionTemplate` is a JSON
 * snapshot of an AI-Quick-Entry-style payload so the rule is portable across
 * schema bumps. Caller (RecurringScheduler) materialises it into a real
 * Transaction at fire time.
 *
 * Phase 4 ships the rule storage + UI. Actual auto-firing on schedule needs
 * WorkManager, which is Phase 5.
 */
@Entity(
    tableName = "recurring_rules",
    indices = [Index(value = ["next_run_date"]), Index(value = ["is_active"])],
)
data class RecurringRuleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "transaction_template")
    val transactionTemplate: String,
    val frequency: RecurringFrequency,
    /** Day-of-month for monthly, day-of-week (1=Mon..7=Sun) for weekly, null otherwise. */
    @ColumnInfo(name = "day_of_period")
    val dayOfPeriod: Int?,
    @ColumnInfo(name = "next_run_date")
    val nextRunDate: Long,
    @ColumnInfo(name = "last_run_date")
    val lastRunDate: Long?,
    @ColumnInfo(name = "auto_confirm")
    val autoConfirm: Boolean,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
