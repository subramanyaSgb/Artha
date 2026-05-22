package com.subramanya.artha.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.domain.model.Category

/**
 * Category picker modal sheet. Shows a flat scrollable list filtered by [type]:
 * parents in bold, children indented under their parent. Search matches name
 * (case-insensitive) across the whole list. Tap a row to commit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    type: CategoryType,
    onSelected: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.category_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.category_picker_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.category_picker_clear))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            val rows = remember(categories, type, query) { buildRows(categories, type, query) }

            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.category_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.category.id }) { row ->
                        CategoryRow(row = row, onClick = { onSelected(row.category) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(row: CategoryRowData, onClick: () -> Unit) {
    val indent = if (row.isChild) 32.dp else 0.dp
    val style = if (row.isChild) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleSmall
    val weight = if (row.isChild) FontWeight.Normal else FontWeight.SemiBold

    Text(
        text = row.category.name,
        style = style.copy(fontWeight = weight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp + indent, end = 24.dp, top = 14.dp, bottom = 14.dp),
    )
}

private data class CategoryRowData(val category: Category, val isChild: Boolean)

/**
 * Filter [all] to [type], group by parent, and flatten to a parent-then-children order.
 * When [query] is non-blank, drop unmatched parents (and dangling children) so the list
 * collapses cleanly around matches.
 */
private fun buildRows(all: List<Category>, type: CategoryType, query: String): List<CategoryRowData> {
    val typed = all.filter { it.type == type }
    val parents = typed.filter { it.parentId == null }.sortedBy { it.displayOrder }
    val childrenByParent = typed.filter { it.parentId != null }.groupBy { it.parentId }

    val needle = query.trim().lowercase()
    val out = ArrayList<CategoryRowData>(typed.size)

    for (parent in parents) {
        val children = (childrenByParent[parent.id] ?: emptyList()).sortedBy { it.displayOrder }
        val parentMatches = needle.isBlank() || parent.name.lowercase().contains(needle)
        val matchedChildren = if (needle.isBlank()) children else children.filter { it.name.lowercase().contains(needle) }

        if (parentMatches) {
            out.add(CategoryRowData(parent, isChild = false))
            for (child in children) out.add(CategoryRowData(child, isChild = true))
        } else if (matchedChildren.isNotEmpty()) {
            // Keep the parent for context, but only show the children that matched.
            out.add(CategoryRowData(parent, isChild = false))
            for (child in matchedChildren) out.add(CategoryRowData(child, isChild = true))
        }
    }
    return out
}
