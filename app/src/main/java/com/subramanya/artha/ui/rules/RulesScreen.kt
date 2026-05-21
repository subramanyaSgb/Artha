package com.subramanya.artha.ui.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: RulesViewModel = viewModel(
        factory = RulesViewModelFactory(app.transactionRuleRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var formMode: RuleFormMode? by remember { mutableStateOf(null) }
    var pendingDelete: TransactionRule? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.rules_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.about_back))
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { formMode = RuleFormMode.Add }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.rules_fab_add))
                }
            },
        ) { padding ->
            if (state.rules.isEmpty()) {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Rule,
                        title = stringResource(R.string.rules_empty),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(state.rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            onTap = { formMode = RuleFormMode.Edit(rule) },
                            onToggle = { vm.toggleActive(rule, it) },
                            onDelete = { pendingDelete = rule },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        RuleFormSheet(
            editing = (mode as? RuleFormMode.Edit)?.rule,
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.rules_delete_confirm_title)) },
            text = {
                Text(
                    if (toDelete.isSystem) stringResource(R.string.rules_delete_system_warning)
                    else stringResource(R.string.rules_delete_confirm_body),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(toDelete)
                    pendingDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.rules_delete_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

internal sealed interface RuleFormMode {
    data object Add : RuleFormMode
    data class Edit(val rule: TransactionRule) : RuleFormMode
}

@Composable
private fun RuleRow(
    rule: TransactionRule,
    onTap: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        headlineContent = { Text(rule.name) },
        supportingContent = {
            val condCount = rule.conditions.items.size
            val actionCount = rule.actions.items.size
            val systemTag = if (rule.isSystem) " · " + stringResource(R.string.rules_row_system_tag) else ""
            Text(
                text = stringResource(R.string.rules_row_subtitle, condCount, actionCount) + systemTag,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Switch(checked = rule.isActive, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.rules_delete_confirm_yes),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}
