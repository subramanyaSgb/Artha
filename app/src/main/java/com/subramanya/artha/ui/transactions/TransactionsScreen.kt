package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.TimeRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    onOpenTransaction: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val vm: TransactionsViewModel = viewModel(
        factory = TransactionsViewModelFactory(
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            categoryRepository = app.categoryRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (state.isSelectionMode) {
                    SelectionTopBar(
                        count = state.selectedIds.size,
                        onClear = vm::clearSelection,
                        onDelete = vm::requestDelete,
                    )
                } else {
                    DefaultTopBar(
                        sort = state.sort,
                        sortMenuOpen = sortMenuOpen,
                        onSortMenuToggle = { sortMenuOpen = it },
                        onSortChanged = vm::onSortChanged,
                    )
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                SearchField(query = state.query, onQueryChanged = vm::onQueryChanged)
                Spacer(modifier = Modifier.height(8.dp))
                FilterRow(state = state, viewModel = vm)
                Spacer(modifier = Modifier.height(8.dp))

                if (state.grouped.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = stringResource(R.string.transactions_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        state.grouped.forEach { group ->
                            item(key = "header-${group.headerKey}") {
                                DayHeader(label = group.headerDisplay)
                            }
                            items(group.transactions, key = { it.id }) { txn ->
                                TransactionRow(
                                    txn = txn,
                                    selected = txn.id in state.selectedIds,
                                    selectionMode = state.isSelectionMode,
                                    onTap = {
                                        if (state.isSelectionMode) vm.toggleSelected(txn.id)
                                        else onOpenTransaction(txn.id)
                                    },
                                    onLongPress = { vm.toggleSelected(txn.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = { Text(stringResource(R.string.transactions_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.transactions_delete_confirm_body, state.selectedIds.size))
            },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) {
                    Text(stringResource(R.string.transactions_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDeleteConfirm) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopBar(
    sort: TransactionSort,
    sortMenuOpen: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortChanged: (TransactionSort) -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.screen_transactions_stub)) },
        actions = {
            Box {
                IconButton(onClick = { onSortMenuToggle(true) }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.transactions_sort_label))
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { onSortMenuToggle(false) }) {
                    TransactionSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayLabel()) },
                            onClick = {
                                onSortChanged(option)
                                onSortMenuToggle(false)
                            },
                            leadingIcon = if (sort == option) {
                                { Icon(Icons.Filled.FilterList, contentDescription = null) }
                            } else null,
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClear: () -> Unit, onDelete: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.transactions_select_count, count)) },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.transactions_select_clear))
            }
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.transactions_select_delete))
            }
        },
    )
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.transactions_search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = null)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FilterRow(state: TransactionsUiState, viewModel: TransactionsViewModel) {
    // Horizontal scroll so long chip labels (e.g. "Religious & Spiritual" category) can't
    // squeeze siblings or wrap to a second line on narrow screens.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Date range
        RangeFilterChip(
            current = state.filter.range,
            onPick = { range -> viewModel.onFilterChanged { it.copy(range = range) } },
        )
        TypeFilterChip(
            current = state.filter.typeFilter,
            onPick = { type -> viewModel.onFilterChanged { it.copy(typeFilter = type) } },
        )
        AccountFilterChip(
            accounts = state.accounts,
            currentId = state.filter.accountId,
            onPick = { id -> viewModel.onFilterChanged { it.copy(accountId = id) } },
        )
        CardFilterChip(
            cards = state.cards,
            currentId = state.filter.cardId,
            onPick = { id -> viewModel.onFilterChanged { it.copy(cardId = id) } },
        )
        CategoryFilterChip(
            categories = state.categories,
            currentId = state.filter.categoryId,
            onPick = { id -> viewModel.onFilterChanged { it.copy(categoryId = id) } },
        )
        if (state.filter != TransactionsFilter() || state.query.isNotEmpty()) {
            TextButton(onClick = { viewModel.clearFilters() }) {
                Text(stringResource(R.string.transactions_filter_clear_all))
            }
        }
    }
}

// ---------------- filter chips with dropdown menus ----------------

@Composable
private fun RangeFilterChip(current: TimeRange, onPick: (TimeRange) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedFilterChip(
            selected = current != TimeRange.ALL_TIME,
            onClick = { expanded = true },
            label = { Text(rangeLabel(current)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(rangeLabel(range)) },
                    onClick = { onPick(range); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun rangeLabel(range: TimeRange): String = when (range) {
    TimeRange.TODAY -> stringResource(R.string.dashboard_section_recent_today)
    TimeRange.THIS_WEEK -> stringResource(R.string.dashboard_section_recent_week)
    TimeRange.THIS_MONTH -> stringResource(R.string.dashboard_section_recent_month)
    TimeRange.ALL_TIME -> stringResource(R.string.transactions_filter_date) + ": " + stringResource(R.string.transactions_filter_any)
}

@Composable
private fun TypeFilterChip(current: TransactionType?, onPick: (TransactionType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedFilterChip(
            selected = current != null,
            onClick = { expanded = true },
            label = {
                Text(current?.displayLabel() ?: stringResource(R.string.transactions_filter_type))
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = { onPick(null); expanded = false },
            )
            TransactionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayLabel()) },
                    onClick = { onPick(type); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun AccountFilterChip(accounts: List<Account>, currentId: String?, onPick: (String?) -> Unit) {
    if (accounts.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentName = currentId?.let { id -> accounts.firstOrNull { it.id == id }?.name }
    Box {
        ElevatedFilterChip(
            selected = currentId != null,
            onClick = { expanded = true },
            label = { Text(currentName ?: stringResource(R.string.transactions_filter_account)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = { onPick(null); expanded = false },
            )
            accounts.forEach { acct ->
                DropdownMenuItem(
                    text = { Text(acct.name) },
                    onClick = { onPick(acct.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CardFilterChip(cards: List<Card>, currentId: String?, onPick: (String?) -> Unit) {
    if (cards.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentName = currentId?.let { id -> cards.firstOrNull { it.id == id }?.name }
    Box {
        ElevatedFilterChip(
            selected = currentId != null,
            onClick = { expanded = true },
            label = { Text(currentName ?: stringResource(R.string.transactions_filter_card)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = { onPick(null); expanded = false },
            )
            cards.forEach { card ->
                DropdownMenuItem(
                    text = { Text(card.name) },
                    onClick = { onPick(card.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(categories: List<Category>, currentId: String?, onPick: (String?) -> Unit) {
    if (categories.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentName = currentId?.let { id -> categories.firstOrNull { it.id == id }?.name }
    Box {
        ElevatedFilterChip(
            selected = currentId != null,
            onClick = { expanded = true },
            label = { Text(currentName ?: stringResource(R.string.transactions_filter_category)) },
        )
        // Top-level only for the dropdown to stay short. Drill-down available in Session 10.
        val parents = remember(categories) { categories.filter { it.parentId == null } }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_filter_any)) },
                onClick = { onPick(null); expanded = false },
            )
            parents.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = { onPick(cat.id); expanded = false },
                )
            }
        }
    }
}

// ---------------- list rows ----------------

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    txn: Transaction,
    selected: Boolean,
    selectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        colors = ListItemDefaults.colors(containerColor = container),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForType(txn.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        headlineContent = { Text(txn.description, maxLines = 1) },
        supportingContent = {
            Text(
                text = txn.type.displayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(
                text = signedAmount(txn),
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor(txn.type),
            )
        },
    )
}

// ---------------- helpers ----------------

@Composable
private fun TransactionType.displayLabel(): String = when (this) {
    TransactionType.EXPENSE -> stringResource(R.string.txn_type_expense)
    TransactionType.INCOME -> stringResource(R.string.txn_type_income)
    TransactionType.TRANSFER -> stringResource(R.string.txn_type_transfer)
    TransactionType.CARD_PAYMENT -> stringResource(R.string.txn_type_card_payment)
    TransactionType.REFUND -> stringResource(R.string.txn_type_refund)
    TransactionType.CASHBACK -> stringResource(R.string.txn_type_cashback)
    TransactionType.INTEREST -> stringResource(R.string.txn_type_interest)
    TransactionType.LOAN_GIVEN -> stringResource(R.string.txn_type_loan_given)
    TransactionType.LOAN_RECEIVED -> stringResource(R.string.txn_type_loan_received)
    TransactionType.GIFT_SENT -> stringResource(R.string.txn_type_gift_sent)
    TransactionType.GIFT_RECEIVED -> stringResource(R.string.txn_type_gift_received)
    TransactionType.ADJUSTMENT -> stringResource(R.string.txn_type_adjustment)
    else -> name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
}

@Composable
private fun TransactionSort.displayLabel(): String = when (this) {
    TransactionSort.DATE_DESC -> stringResource(R.string.transactions_sort_date_desc)
    TransactionSort.DATE_ASC -> stringResource(R.string.transactions_sort_date_asc)
    TransactionSort.AMOUNT_DESC -> stringResource(R.string.transactions_sort_amount_desc)
    TransactionSort.AMOUNT_ASC -> stringResource(R.string.transactions_sort_amount_asc)
}

private fun iconForType(type: TransactionType) = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    else -> Icons.Filled.AccountBalance
}

@Composable
private fun amountColor(type: TransactionType): Color = when (type) {
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
    -> MaterialTheme.colorScheme.primary
    TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
    -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when (txn.type) {
        TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
        TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
        -> "+$abs"
        TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
        -> "−$abs"
        else -> abs
    }
}

