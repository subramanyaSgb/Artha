package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.BudgetPeriod
import com.subramanya.artha.data.entity.enums.BudgetScope

/**
 * PRD §7.13 Budget. Spent value isn't stored — it's derived per-period from the
 * transaction log by [com.subramanya.artha.data.balance.BudgetCalculator].
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["scope"]),
        Index(value = ["category_id"]),
        Index(value = ["is_active"]),
    ],
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val scope: BudgetScope,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    val amount: Double,
    val period: BudgetPeriod,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "alert_threshold_percent")
    val alertThresholdPercent: Int,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
