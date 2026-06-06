package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Insurance-type catalogue (Phase 3). Replaces InsuranceType enum. */
@Entity(tableName = "insurance_type")
data class InsuranceTypeEntity(
    @PrimaryKey val id: String,
    val label: String,
    @ColumnInfo(name = "is_builtin") val isBuiltin: Boolean,
    @ColumnInfo(name = "is_hidden", defaultValue = "0") val isHidden: Boolean = false,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)
