package com.subramanya.artha.ui.categories

import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.domain.model.Category

/** Single visible tab. Categories are filtered to one type at a time to keep the tree shallow. */
data class CategoriesUiState(
    val type: CategoryType = CategoryType.EXPENSE,
    val parents: List<Category> = emptyList(),
    /** Lookup: parentId → its children, already sorted by displayOrder. */
    val childrenByParent: Map<String, List<Category>> = emptyMap(),
    val expandedParentIds: Set<String> = emptySet(),
)
