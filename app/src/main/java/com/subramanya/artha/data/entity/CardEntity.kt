package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.CardNetwork


@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linked_account_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["linked_account_id"])],
)
data class CardEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val issuer: String?,
    val network: CardNetwork,
    @ColumnInfo(name = "card_number_last4")
    val cardNumberLast4: String?,
    @ColumnInfo(name = "credit_limit")
    val creditLimit: Double?,
    @ColumnInfo(name = "statement_day_of_month")
    val statementDayOfMonth: Int?,
    @ColumnInfo(name = "due_day_of_month")
    val dueDayOfMonth: Int?,
    @ColumnInfo(name = "linked_account_id")
    val linkedAccountId: String?,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "card_image_uri")
    val cardImageUri: String? = null,
)
