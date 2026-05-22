package com.subramanya.artha.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.domain.model.Category
import java.util.UUID

/**
 * Add / Edit category modal. System categories keep their fixed `type` (the chip
 * row stays disabled) but their name and color remain editable per PRD §7.12.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryFormSheet(
    editing: Category?,
    defaultType: CategoryType,
    parentCandidates: List<Category>,
    onSave: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: defaultType) }
    var parentId by remember(editing) { mutableStateOf(editing?.parentId) }
    var icon by remember(editing) { mutableStateOf(editing?.icon ?: ICONS.first()) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }
    var showErrors by remember { mutableStateOf(false) }

    val isSystem = editing?.isSystem == true
    val isValid = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(
                    if (editing == null) R.string.category_form_add_title else R.string.category_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (isSystem) {
                Text(
                    text = stringResource(R.string.category_form_system_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.category_form_name_label)) },
                placeholder = { Text(stringResource(R.string.category_form_name_placeholder)) },
                isError = showErrors && name.isBlank(),
                supportingText = {
                    if (showErrors && name.isBlank()) {
                        Text(stringResource(R.string.category_form_validation_name))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Type row — disabled for system categories.
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.category_form_type_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { if (!isSystem) type = option },
                        enabled = !isSystem,
                        label = { Text(option.name.lowercase().replaceFirstChar { it.titlecase() }) },
                    )
                }
            }

            // Parent picker — only show top-level categories of the same type, excluding self
            // (would create a cycle). For system parents this also stays available.
            val availableParents = parentCandidates.filter {
                it.type == type && it.parentId == null && it.id != editing?.id
            }
            if (availableParents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.category_form_parent_label), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = parentId == null,
                        onClick = { parentId = null },
                        label = { Text(stringResource(R.string.category_form_parent_none)) },
                    )
                    availableParents.forEach { parent ->
                        FilterChip(
                            selected = parentId == parent.id,
                            onClick = { parentId = parent.id },
                            label = { Text(parent.name) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.category_form_icon_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ICONS.forEach { iconName ->
                    FilterChip(
                        selected = icon == iconName,
                        onClick = { icon = iconName },
                        label = { Text(iconName.replace('_', ' ')) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.category_form_color_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.forEach { swatch ->
                    val ringWidth = if (color == swatch) 3.dp else 0.dp
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .border(width = ringWidth, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                            .clickable { color = swatch },
                    ) { }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@Button
                    }
                    val displayOrder = editing?.displayOrder ?: nextDisplayOrder()
                    val resolved = Category(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        parentId = parentId,
                        type = if (isSystem) editing!!.type else type,
                        icon = icon,
                        color = color,
                        isSystem = editing?.isSystem ?: false,
                        displayOrder = displayOrder,
                    )
                    onSave(resolved)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.category_form_save)) }
        }
    }
}

private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

/** A small Phase 1 palette — full picker is Session 10. */
private val PALETTE: List<Long> = listOf(
    0xFFEF4444L, // red
    0xFF10B981L, // emerald
    0xFF6366F1L, // indigo
    0xFFF59E0BL, // amber
    0xFFEC4899L, // pink
    0xFF6D28D9L, // violet
)

private val ICONS: List<String> = listOf(
    "category",
    "shopping_bag",
    "restaurant",
    "directions_car",
    "receipt_long",
    "medical_services",
    "celebration",
    "school",
)
