package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val institution: String?,
    @ColumnInfo(name = "account_number_last4")
    val accountNumberLast4: String?,
    @ColumnInfo(name = "opening_balance")
    val openingBalance: Double,
    val currency: String,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
