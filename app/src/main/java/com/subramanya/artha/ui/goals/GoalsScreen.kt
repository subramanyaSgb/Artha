package com.subramanya.artha.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.subramanya.artha.domain.model.Goal
import com.subramanya.artha.domain.model.GoalWithProgress
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val items by app.goalRepository.observeAllWithProgress()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by app.accountRepository.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val investments by app.investmentRepository.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: Goal? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = { Text(stringResource(R.string.goals_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.about_back))
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { formMode = FormMode.Add }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.goals_fab_add))
                }
            },
        ) { padding ->
            if (items.isEmpty()) {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(icon = Icons.Filled.Flag, title = stringResource(R.string.goals_empty))
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(items, key = { it.goal.id }) { row ->
                        GoalRow(
                            row = row,
                            onTap = { formMode = FormMode.Edit(row.goal) },
                            onDelete = { pendingDelete = row.goal },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        GoalFormSheet(
            editing = (mode as? FormMode.Edit)?.goal,
            accountNames = accounts.associate { it.id to it.name },
            investmentNames = investments.associate { it.id to it.name },
            onSave = { resolved -> scope.launch { app.goalRepository.upsert(resolved); formMode = null } },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.goals_delete_confirm_title)) },
            text = { Text(stringResource(R.string.goals_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.goalRepository.delete(toDelete); pendingDelete = null }
                }) { Text(stringResource(R.string.goals_delete_confirm_yes), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val goal: Goal) : FormMode
}

@Composable
private fun GoalRow(row: GoalWithProgress, onTap: () -> Unit, onDelete: () -> Unit) {
    val ratio = (row.percentDone / 100.0).toFloat().coerceIn(0f, 1f)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onTap)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.goal.name, style = MaterialTheme.typography.titleSmall)
                    val subtitle = buildList {
                        add(IndianNumberFormat.format(row.goal.targetAmount))
                        row.daysRemaining?.let {
                            add(stringResource(R.string.goals_days_remaining, it))
                        }
                    }.joinToString(" · ")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                text = IndianNumberFormat.format(row.currentAmount) +
                    " · ${"%+.1f".format(row.percentDone)}%",
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalFormSheet(
    editing: Goal?,
    accountNames: Map<String, String>,
    investmentNames: Map<String, String>,
    onSave: (Goal) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var targetText by remember(editing) { mutableStateOf(editing?.targetAmount?.toPlainString() ?: "") }
    val pickedAccounts = remember(editing) {
        mutableStateOf(editing?.linkedAccountIds.orEmpty().toMutableSet())
    }
    val pickedInvestments = remember(editing) {
        mutableStateOf(editing?.linkedInvestmentIds.orEmpty().toMutableSet())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(
                text = stringResource(
                    if (editing == null) R.string.goals_form_add_title else R.string.goals_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.goals_form_name_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            OutlinedTextField(
                value = targetText,
                onValueChange = { v ->
                    targetText = v.filterIndexed { i, c -> c.isDigit() || (c == '.' && v.indexOf('.') == i) }
                },
                singleLine = true,
                prefix = { Text("₹") },
                label = { Text(stringResource(R.string.goals_form_target_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.goals_form_linked_accounts), style = MaterialTheme.typography.labelLarge)
            MultiSelectChips(
                options = accountNames,
                selected = pickedAccounts.value,
                onToggle = { id ->
                    pickedAccounts.value =
                        (if (id in pickedAccounts.value) pickedAccounts.value - id else pickedAccounts.value + id).toMutableSet()
                },
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.goals_form_linked_investments), style = MaterialTheme.typography.labelLarge)
            MultiSelectChips(
                options = investmentNames,
                selected = pickedInvestments.value,
                onToggle = { id ->
                    pickedInvestments.value =
                        (if (id in pickedInvestments.value) pickedInvestments.value - id else pickedInvestments.value + id).toMutableSet()
                },
            )

            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: return@Button
                    val now = System.currentTimeMillis()
                    onSave(
                        Goal(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim().ifBlank { "Goal" },
                            targetAmount = target,
                            targetDate = editing?.targetDate,
                            linkedAccountIds = pickedAccounts.value.toList(),
                            linkedInvestmentIds = pickedInvestments.value.toList(),
                            icon = editing?.icon ?: "flag",
                            color = editing?.color ?: 0xFF0F766EL,
                            isAchieved = editing?.isAchieved ?: false,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
                enabled = name.isNotBlank() && targetText.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text(stringResource(R.string.common_save)) }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MultiSelectChips(
    options: Map<String, String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (options.isEmpty()) {
        Text(
            text = stringResource(R.string.goals_form_no_options),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            androidx.compose.material3.FilterChip(
                selected = id in selected,
                onClick = { onToggle(id) },
                label = { Text(label) },
            )
        }
    }
}

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
