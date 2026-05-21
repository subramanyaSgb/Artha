package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PRD §7.14 Goal. linkedAccountIds + linkedInvestmentIds are JSON-serialised
 * lists of ids so we avoid an N-to-N join table for what's effectively a tag-set.
 *
 * `currentAmount` is computed by [com.subramanya.artha.data.balance.GoalCalculator]
 * — sum of current balances of linked accounts + current values of linked
 * investments. Never stored.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,
    @ColumnInfo(name = "target_date")
    val targetDate: Long?,
    /** Stored as JSON array of ids, e.g. `["acct-1","acct-2"]`. Empty string = none. */
    @ColumnInfo(name = "linked_account_ids")
    val linkedAccountIdsJson: String,
    @ColumnInfo(name = "linked_investment_ids")
    val linkedInvestmentIdsJson: String,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "is_achieved")
    val isAchieved: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
