package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.PersonRelation

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val relation: PersonRelation,
    val contact: String?,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
