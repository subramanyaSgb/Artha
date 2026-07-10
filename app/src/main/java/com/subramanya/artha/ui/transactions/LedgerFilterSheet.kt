package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.ui.common.ArthaDatePickerDialog
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.transaction.CategoryPickerSheet
import com.subramanya.artha.utils.TimeRange
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LedgerFilterSheet(
    current: TransactionsFilter,
    accounts: List<Account>,
    categories: List<Category>,
    tags: List<Tag>,
    onApply: (TransactionsFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(current) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ledger_filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { draft = TransactionsFilter() }) {
                    Text(stringResource(R.string.transactions_filter_clear_all))
                }
            }
            Spacer(Modifier.height(16.dp))

            FilterSectionLabel(stringResource(R.string.transactions_filter_date))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = draft.range == range,
                        onClick = { draft = draft.copy(range = range) },
                        label = { Text(rangeLabel(range)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Teal500.copy(alpha = 0.15f),
                            selectedLabelColor = Teal500,
                        ),
                    )
                }
            }
            if (draft.range == TimeRange.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.ledger_filter_custom_start),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = draft.customDateStart?.let { formatShortDate(it) }
                                    ?: stringResource(R.string.ledger_filter_pick_start_date),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.ledger_filter_custom_end),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = draft.customDateEnd?.let { formatShortDate(it) }
                                    ?: stringResource(R.string.ledger_filter_pick_end_date),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            FilterSectionLabel(stringResource(R.string.transactions_filter_type))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.typeFilter == null,
                    onClick = { draft = draft.copy(typeFilter = null) },
                    label = { Text(stringResource(R.string.transactions_filter_any)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Teal500.copy(alpha = 0.15f),
                        selectedLabelColor = Teal500,
                    ),
                )
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = draft.typeFilter == type,
                        onClick = { draft = draft.copy(typeFilter = type) },
                        label = { Text(type.displayLabel()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Teal500.copy(alpha = 0.15f),
                            selectedLabelColor = Teal500,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            FilterSectionLabel(stringResource(R.string.transactions_filter_category))
            Spacer(Modifier.height(8.dp))
            val selectedCatName = draft.categoryId
                ?.let { id -> categories.firstOrNull { it.id == id }?.name }
                ?: stringResource(R.string.transactions_filter_any)
            OutlinedButton(
                onClick = { showCategoryPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(selectedCatName, modifier = Modifier.weight(1f))
                if (draft.categoryId != null) {
                    TextButton(onClick = { draft = draft.copy(categoryId = null) }) {
                        Text("✕")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (accounts.isNotEmpty()) {
                FilterSectionLabel(stringResource(R.string.transactions_filter_account))
                Spacer(Modifier.height(8.dp))
                FilterSimpleDropdown(
                    label = accounts.firstOrNull { it.id == draft.accountId }?.name
                        ?: stringResource(R.string.transactions_filter_any),
                    options = listOf(null to stringResource(R.string.transactions_filter_any)) +
                        accounts.map { it.id to it.name },
                    onSelect = { draft = draft.copy(accountId = it) },
                )
                Spacer(Modifier.height(16.dp))
            }

            if (tags.isNotEmpty()) {
                FilterSectionLabel(stringResource(R.string.transactions_filter_tag))
                Spacer(Modifier.height(8.dp))
                FilterSimpleDropdown(
                    label = tags.firstOrNull { it.id == draft.tagId }?.name
                        ?: stringResource(R.string.transactions_filter_any),
                    options = listOf(null to stringResource(R.string.transactions_filter_any)) +
                        tags.map { it.id to it.name },
                    onSelect = { draft = draft.copy(tagId = it) },
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onApply(draft); onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700),
            ) {
                Text(
                    stringResource(R.string.ledger_filter_apply),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = categories,
            type = CategoryType.EXPENSE,
            onSelected = { draft = draft.copy(categoryId = it.id); showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false },
        )
    }
    if (showStartDatePicker) {
        ArthaDatePickerDialog(
            initialMillis = draft.customDateStart ?: Clock.System.now().toEpochMilliseconds(),
            onConfirm = { millis ->
                val currentEnd = draft.customDateEnd
                val newEnd = if (currentEnd != null && currentEnd < millis) millis else currentEnd
                draft = draft.copy(customDateStart = millis, customDateEnd = newEnd)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
        )
    }
    if (showEndDatePicker) {
        ArthaDatePickerDialog(
            initialMillis = draft.customDateEnd ?: Clock.System.now().toEpochMilliseconds(),
            onConfirm = { millis ->
                val endOfDay = endOfDayMillis(millis)
                val currentStart = draft.customDateStart
                val newStart = if (currentStart != null && currentStart > endOfDay) endOfDay else currentStart
                draft = draft.copy(customDateEnd = endOfDay, customDateStart = newStart)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
        )
    }
}

private fun formatShortDate(millis: Long): String {
    val tz = TimeZone.currentSystemDefault()
    val d = Instant.fromEpochMilliseconds(millis).toLocalDateTime(tz).date
    return "${d.dayOfMonth} ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} ${d.year}"
}

private fun endOfDayMillis(dayStartMillis: Long): Long {
    // The Material3 DatePicker returns UTC midnight; add 23h 59m 59.999s to cover the whole day.
    return dayStartMillis + 24L * 60 * 60 * 1000 - 1
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FilterSimpleDropdown(
    label: String,
    options: List<Pair<String?, String>>,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) { Text(label, modifier = Modifier.weight(1f)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}
