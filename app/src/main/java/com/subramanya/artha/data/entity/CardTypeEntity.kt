package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Card-type catalogue (Phase 3). Replaces CardType enum. CREDIT id still used in logic. */
@Entity(tableName = "card_type")
data class CardTypeEntity(
    @PrimaryKey val id: String,
    val label: String,
    @ColumnInfo(name = "is_builtin") val isBuiltin: Boolean,
    @ColumnInfo(name = "is_hidden", defaultValue = "0") val isHidden: Boolean = false,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)
