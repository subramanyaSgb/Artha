package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.CategoryType

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["parent_id"])],
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "parent_id")
    val parentId: String?,
    val type: CategoryType,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
)
