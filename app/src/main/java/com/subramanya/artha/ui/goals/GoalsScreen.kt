package com.subramanya.artha.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Goal
import com.subramanya.artha.domain.model.GoalWithProgress
import com.subramanya.artha.ui.common.Chhatri
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
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

    Surface(color = com.subramanya.artha.ui.theme.Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = com.subramanya.artha.ui.theme.Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    containerColor = com.subramanya.artha.ui.theme.Teal700,
                    contentColor = com.subramanya.artha.ui.theme.Text1,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.goals_fab_add)) },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                com.subramanya.artha.ui.common.InlineTopBar(
                    title = stringResource(R.string.goals_title),
                    onBack = onBack,
                )
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(icon = Icons.Filled.Flag, title = stringResource(R.string.goals_empty))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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

/**
 * HANDOFF §3.7 — Goal card carries a faint Chhatri silhouette at 0.06 in the
 * bottom-right corner; the rest is editorial body + progress bar. Mono numerals
 * for amounts, sage/ochre/coral tinting based on completion.
 */
@Composable
private fun GoalRow(row: GoalWithProgress, onTap: () -> Unit, onDelete: () -> Unit) {
    val ratio = (row.percentDone / 100.0).toFloat().coerceIn(0f, 1f)
    val isComplete = ratio >= 1f
    val tint = when {
        isComplete -> Income
        ratio >= 0.7f -> Teal300
        ratio >= 0.4f -> Ochre
        else -> Text2
    }
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                color = if (isComplete) LineTeal else Line1,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onTap),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Faint chhatri silhouette in the bottom-right corner — HANDOFF §3.7.
            Chhatri(
                tint = Teal500.copy(alpha = 0.06f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
                    .size(80.dp),
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface4),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.goal.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Text1,
                        )
                        val subtitle = buildList {
                            add("Target ${IndianNumberFormat.format(row.goal.targetAmount)}")
                            row.daysRemaining?.let {
                                add(stringResource(R.string.goals_days_remaining, it))
                            }
                        }.joinToString(" · ")
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFeatureSettings = "tnum, lnum",
                            ),
                            color = Text3,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Text3,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Surface4),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(tint),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = IndianNumberFormat.format(row.currentAmount),
                        style = TextStyle(
                            fontFamily = IbmPlexMono,
                            fontSize = 12.sp,
                            color = Text1,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                    )
                    Text(
                        text = "${"%.1f".format(row.percentDone)}%",
                        style = TextStyle(
                            fontFamily = IbmPlexMono,
                            fontSize = 12.sp,
                            color = tint,
                            fontFeatureSettings = "tnum, lnum",
                        ),
                    )
                }
            }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
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
