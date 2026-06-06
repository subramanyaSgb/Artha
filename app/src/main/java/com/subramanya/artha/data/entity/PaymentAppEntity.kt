package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catalogue of payment apps the user can pick on a transaction. Replaces the old fixed
 * `PaymentApp` enum (Phase 2 of the configurable pick-lists work). The 10 former enum values
 * are seeded as built-ins (`isBuiltin = 1`) whose ids are the old enum names, so existing
 * `transactions.payment_app` rows (which stored `enum.name`) resolve unchanged. The user can
 * add custom apps (UUID id, `isBuiltin = 0`) and hide built-ins — built-ins are never deleted,
 * so a backup / old transaction always resolves its label.
 */
@Entity(tableName = "payment_app")
data class PaymentAppEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    @ColumnInfo(name = "is_builtin")
    val isBuiltin: Boolean,
    @ColumnInfo(name = "is_hidden", defaultValue = "0")
    val isHidden: Boolean = false,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
)
