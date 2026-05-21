package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.CategoryType

data class Category(
    val id: String,
    val name: String,
    val parentId: String?,
    val type: CategoryType,
    val icon: String,
    val color: Long,
    val isSystem: Boolean,
    val displayOrder: Int,
)
