package com.subramanya.artha.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.BudgetPeriod
import com.subramanya.artha.data.entity.enums.BudgetScope
import com.subramanya.artha.domain.model.Budget
import com.subramanya.artha.domain.model.BudgetWithProgress
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val items by app.budgetRepository.observeActiveWithProgress()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Budget? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.budgets_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.about_back))
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { formMode = FormMode.Add }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budgets_fab_add))
                }
            },
        ) { padding ->
            if (items.isEmpty()) {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = stringResource(R.string.budgets_empty),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(items, key = { it.budget.id }) { row ->
                        BudgetRow(
                            row = row,
                            onTap = { formMode = FormMode.Edit(row.budget) },
                            onDelete = { pendingDelete = row.budget },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        BudgetFormSheet(
            editing = (mode as? FormMode.Edit)?.budget,
            onSave = { resolved ->
                scope.launch { app.budgetRepository.upsert(resolved); formMode = null }
            },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.budgets_delete_confirm_title)) },
            text = { Text(stringResource(R.string.budgets_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.budgetRepository.delete(toDelete); pendingDelete = null }
                }) {
                    Text(stringResource(R.string.budgets_delete_confirm_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val budget: Budget) : FormMode
}

@Composable
private fun BudgetRow(
    row: BudgetWithProgress,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val ratio = if (row.budget.amount == 0.0) 0f else (row.spent / row.budget.amount).toFloat()
    val color = when {
        ratio >= 1.0f -> MaterialTheme.colorScheme.error
        ratio >= (row.budget.alertThresholdPercent / 100f) -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onTap),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.budget.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = row.budget.scope.label() + " · " + row.budget.period.label() +
                            " · " + stringResource(R.string.budgets_days_left, row.daysRemainingInPeriod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = color,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = IndianNumberFormat.format(row.spent) + " / " + IndianNumberFormat.format(row.budget.amount),
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.Medium),
                color = color,
            )
        }
    }
}

@Composable
private fun BudgetScope.label(): String = when (this) {
    BudgetScope.OVERALL -> stringResource(R.string.budget_scope_overall)
    BudgetScope.CATEGORY -> stringResource(R.string.budget_scope_category)
}

@Composable
private fun BudgetPeriod.label(): String = when (this) {
    BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
    BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
    BudgetPeriod.YEARLY -> stringResource(R.string.budget_period_yearly)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BudgetFormSheet(
    editing: Budget?,
    onSave: (Budget) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var scope by remember(editing) { mutableStateOf(editing?.scope ?: BudgetScope.OVERALL) }
    var period by remember(editing) { mutableStateOf(editing?.period ?: BudgetPeriod.MONTHLY) }
    var amountText by remember(editing) { mutableStateOf(editing?.amount?.toPlainString() ?: "") }
    var categoryId by remember(editing) { mutableStateOf(editing?.categoryId.orEmpty()) }
    var threshold by remember(editing) { mutableStateOf((editing?.alertThresholdPercent ?: 80).toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                text = stringResource(
                    if (editing == null) R.string.budgets_form_add_title else R.string.budgets_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.budgets_form_name_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.budgets_form_scope_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetScope.entries.forEach { opt ->
                    FilterChip(selected = scope == opt, onClick = { scope = opt }, label = { Text(opt.label()) })
                }
            }

            if (scope == BudgetScope.CATEGORY) {
                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.budgets_form_category_id_label)) },
                    placeholder = { Text("cat_food_drink") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.budgets_form_period_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetPeriod.entries.forEach { opt ->
                    FilterChip(selected = period == opt, onClick = { period = opt }, label = { Text(opt.label()) })
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { v ->
                    amountText = v.filterIndexed { i, c -> c.isDigit() || (c == '.' && v.indexOf('.') == i) }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.budgets_form_amount_label)) },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            OutlinedTextField(
                value = threshold,
                onValueChange = { v -> threshold = v.filter { it.isDigit() }.take(3) },
                singleLine = true,
                label = { Text(stringResource(R.string.budgets_form_threshold_label)) },
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    val t = threshold.toIntOrNull()?.coerceIn(1, 100) ?: 80
                    val now = System.currentTimeMillis()
                    onSave(
                        Budget(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim().ifBlank { "Budget" },
                            scope = scope,
                            categoryId = categoryId.takeIf { it.isNotBlank() && scope == BudgetScope.CATEGORY },
                            amount = amount,
                            period = period,
                            startDate = editing?.startDate ?: now,
                            alertThresholdPercent = t,
                            isActive = editing?.isActive ?: true,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
                enabled = amountText.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text(stringResource(R.string.common_save)) }
        }
    }
}

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
