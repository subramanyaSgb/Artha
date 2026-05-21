package com.subramanya.artha.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.AccountWithBalance
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier,
    onOpenAccount: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: AccountsViewModel = viewModel(
        factory = AccountsViewModelFactory(app.accountRepository),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var overflowOpen by remember { mutableStateOf(false) }

    /** Null = sheet closed. The Account value or sentinel decides Add vs Edit mode. */
    var formMode: FormMode? by remember { mutableStateOf(null) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                state.isReorderMode -> stringResource(R.string.accounts_reorder_hint)
                                state.view == AccountsView.ARCHIVED -> stringResource(R.string.accounts_section_archived)
                                else -> stringResource(R.string.accounts_title)
                            },
                        )
                    },
                    actions = {
                        if (state.isReorderMode) {
                            TextButton(onClick = vm::exitReorderMode) {
                                Icon(Icons.Filled.Done, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.accounts_reorder_done),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        } else {
                            Box {
                                IconButton(onClick = { overflowOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                                    if (state.view == AccountsView.ACTIVE) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.accounts_menu_show_archived)) },
                                            onClick = {
                                                vm.showArchived()
                                                overflowOpen = false
                                            },
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.accounts_menu_back_active)) },
                                            onClick = {
                                                vm.showActive()
                                                overflowOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (state.view == AccountsView.ACTIVE && !state.isReorderMode) {
                    FloatingActionButton(onClick = { formMode = FormMode.Add }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accounts_fab_add))
                    }
                }
            },
        ) { padding ->
            val rows = state.shownRows
            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.AccountBalance,
                        title = stringResource(
                            if (state.view == AccountsView.ACTIVE) R.string.accounts_empty_active
                            else R.string.accounts_empty_archived,
                        ),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                    items(rows, key = { it.account.id }) { row ->
                        if (state.view == AccountsView.ACTIVE) {
                            ActiveAccountRow(
                                row = row,
                                reorderMode = state.isReorderMode,
                                canMoveUp = rows.first() != row,
                                canMoveDown = rows.last() != row,
                                onTap = {
                                    if (!state.isReorderMode) onOpenAccount(row.account.id)
                                },
                                onLongPress = vm::enterReorderMode,
                                onMoveUp = { vm.moveUp(row.account) },
                                onMoveDown = { vm.moveDown(row.account) },
                                onEdit = { formMode = FormMode.Edit(row.account) },
                                onArchive = { vm.archive(row.account) },
                            )
                        } else {
                            ArchivedAccountRow(
                                row = row,
                                onRestore = { vm.restore(row.account) },
                            )
                        }
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        AccountFormSheet(
            editing = (mode as? FormMode.Edit)?.account,
            onDismiss = { formMode = null },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val account: Account) : FormMode
}

// ---------------- rows ----------------

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ActiveAccountRow(
    row: AccountWithBalance,
    reorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        colors = ListItemDefaults.colors(),
        leadingContent = { AccountAvatar(color = row.account.color) },
        headlineContent = { Text(row.account.name, maxLines = 1) },
        supportingContent = {
            val subtitle = formatSubtitle(row.account)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            if (reorderMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(R.string.accounts_action_move_up),
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = stringResource(R.string.accounts_action_move_down),
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = IndianNumberFormat.format(row.currentBalance),
                        style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.account_detail_action_edit)) },
                                onClick = { menuOpen = false; onEdit() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.account_detail_action_archive)) },
                                onClick = { menuOpen = false; onArchive() },
                                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ArchivedAccountRow(
    row: AccountWithBalance,
    onRestore: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { AccountAvatar(color = row.account.color) },
        headlineContent = { Text(row.account.name, maxLines = 1) },
        supportingContent = {
            val subtitle = formatSubtitle(row.account)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            TextButton(onClick = onRestore) {
                Icon(Icons.Filled.Unarchive, contentDescription = null)
                Text(
                    text = stringResource(R.string.accounts_action_restore),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        },
    )
}

@Composable
private fun AccountAvatar(color: Long) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = Color.White,
        )
    }
}

private fun formatSubtitle(account: Account): String? {
    val pieces = buildList {
        if (!account.institution.isNullOrBlank()) add(account.institution!!)
        if (!account.accountNumberLast4.isNullOrBlank()) add("••${account.accountNumberLast4}")
    }
    return pieces.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
