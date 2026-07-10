package com.subramanya.artha.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.MonoMeta
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.ui.theme.expenseSoftFill
import com.subramanya.artha.ui.theme.incomeSoftFill
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.TimeRange
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            tagRepository = app.tagRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    // Search text held locally so it updates synchronously with each keystroke; the VM is
    // fed separately to run the filter (which re-derives the whole list on Default). Binding
    // the field straight to the VM's StateFlow would lag the IME and jumble fast typing.
    var query by rememberSaveable { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.isSelectionMode) {
                    SelectionTopBar(
                        count = state.selectedIds.size,
                        onClear = vm::clearSelection,
                        onDelete = vm::requestDelete,
                    )
                } else {
                    val customStart = state.filter.customDateStart
                    val customEnd = state.filter.customDateEnd
                    LedgerHeader(
                        rangeLabel = if (state.filter.range == TimeRange.CUSTOM &&
                            customStart != null && customEnd != null
                        ) {
                            "${formatChipDate(customStart)} – ${formatChipDate(customEnd)}"
                        } else {
                            rangeDisplay(state.filter.range)
                        },
                        sort = state.sort,
                        activeFilterCount = run {
                            var count = 0
                            if (state.filter.range != TimeRange.ALL_TIME) count++
                            if (state.filter.typeFilter != null) count++
                            if (state.filter.accountId != null) count++
                            if (state.filter.cardId != null) count++
                            if (state.filter.categoryId != null) count++
                            if (state.filter.tagId != null) count++
                            count
                        },

                        sortMenuOpen = sortMenuOpen,
                        onSortMenuToggle = { sortMenuOpen = it },
                        onSortChanged = vm::onSortChanged,
                        onFilterClick = { showFilterSheet = true },
                    )
                }

                TotalsStrip(inSum = state.inSum, outSum = state.outSum, net = state.net)

                SearchField(
                    query = query,
                    onQueryChanged = { query = it; vm.onQueryChanged(it) },
                )
                Spacer(Modifier.height(4.dp))
                ActiveFiltersStrip(
                    filter = state.filter,
                    accounts = state.accounts,
                    cards = state.cards,
                    categories = state.categories,
                    tags = state.tags,
                    queryActive = query.isNotEmpty(),
                    onRemoveRange = { vm.onFilterChanged { it.copy(range = TimeRange.ALL_TIME, customDateStart = null, customDateEnd = null) } },
                    onRemoveType = { vm.onFilterChanged { it.copy(typeFilter = null) } },
                    onRemoveAccount = { vm.onFilterChanged { it.copy(accountId = null) } },
                    onRemoveCard = { vm.onFilterChanged { it.copy(cardId = null) } },
                    onRemoveCategory = { vm.onFilterChanged { it.copy(categoryId = null) } },
                    onRemoveTag = { vm.onFilterChanged { it.copy(tagId = null) } },
                    onClearAll = { query = ""; vm.clearFilters() },
                )
                Spacer(Modifier.height(4.dp))
                if (state.count > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.transactions_count, state.count, state.count),
                        style = MaterialTheme.typography.labelSmall,
                        color = Text3,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }

                if (state.rows.isEmpty()) {
                    // Distinguish "no matches for this filter/search" from "no transactions at all".
                    val filtersActive = state.filter != TransactionsFilter() || query.isNotEmpty()
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Inbox, contentDescription = null, tint = Text3, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    if (filtersActive) R.string.transactions_empty_filtered
                                    else R.string.transactions_empty,
                                ),
                                color = Text3,
                            )
                            if (filtersActive) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { query = ""; vm.clearFilters() }) {
                                    Text(stringResource(R.string.transactions_filter_clear_all))
                                }
                            }
                        }
                    }
                    return@Column
                }

                // Each transaction is its own keyed lazy item (true virtualization) — the day-card
                // look is preserved by rounding only the first/last row of each day and drawing a
                // divider between rows. Selecting/scrolling recomposes one row, not the whole day.
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(state.rows, key = { it.key }) { item ->
                        when (item) {
                            is LedgerListItem.DayHeader ->
                                DayHeader(label = item.display, sum = item.daySum)
                            is LedgerListItem.Entry -> {
                                val shape = when {
                                    item.isFirstInDay && item.isLastInDay -> RoundedCornerShape(14.dp)
                                    item.isFirstInDay -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                                    item.isLastInDay -> RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                    ) {
                                        TransactionRow(
                                            txn = item.txn,
                                            category = item.category,
                                            selected = item.txn.id in state.selectedIds,
                                            selectionMode = state.isSelectionMode,
                                            onTap = {
                                                if (state.isSelectionMode) vm.toggleSelected(item.txn.id)
                                                else onOpenTransaction(item.txn.id)
                                            },
                                            onLongPress = { vm.toggleSelected(item.txn.id) },
                                        )
                                        if (!item.isLastInDay) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 56.dp)
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                            )
                                        }
                                    }
                                    if (item.isLastInDay) Spacer(Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Quick add: open the standard Add Transaction sheet straight from the Ledger.
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ledger_add_transaction))
        }
    }

    if (showFilterSheet) {
        LedgerFilterSheet(
            current = state.filter,
            accounts = state.accounts,
            categories = state.categories,
            tags = state.tags,
            onApply = { newFilter -> vm.onFilterChanged { newFilter } },
            onDismiss = { showFilterSheet = false },
        )
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.transactions_delete_confirm_title),
            text = stringResource(R.string.transactions_delete_confirm_body, state.selectedIds.size),
            confirmLabel = stringResource(R.string.transactions_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = vm::confirmDelete,
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }

    if (showAddSheet) {
        val txnVm: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModelFactory(
                accountRepository = app.accountRepository,
                cardRepository = app.cardRepository,
                categoryRepository = app.categoryRepository,
                personRepository = app.personRepository,
                tagRepository = app.tagRepository,
                transactionRepository = app.transactionRepository,
                transactionRuleRepository = app.transactionRuleRepository,
                investmentRepository = app.investmentRepository,
                settingsPreferences = app.settingsPreferences,
                paymentAppRepository = app.paymentAppRepository,
            ),
        )
        AddTransactionSheet(viewModel = txnVm, onDismiss = { showAddSheet = false })
    }
}

// ───────────────────────────── Header (Editorial title + sort) ───────────────

@Composable
private fun LedgerHeader(
    rangeLabel: String,
    sort: TransactionSort,
    activeFilterCount: Int,
    sortMenuOpen: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortChanged: (TransactionSort) -> Unit,
    onFilterClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ledger_title).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = rangeLabel,
                fontFamily = InstrumentSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        // Filter button with active-count badge
        Box {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.ledger_filter_sheet_title))
            }
            if (activeFilterCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Teal500)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$activeFilterCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                    )
                }
            }
        }
        // Sort button
        Box {
            IconButton(onClick = { onSortMenuToggle(true) }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.transactions_sort_label))
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { onSortMenuToggle(false) }) {
                TransactionSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayLabel()) },
                        onClick = { onSortChanged(option); onSortMenuToggle(false) },
                        leadingIcon = if (sort == option) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── In / Out / Net strip ──────────────────────────

@Composable
private fun TotalsStrip(inSum: Double, outSum: Double, net: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TotalCell(label = stringResource(R.string.ledger_total_in).uppercase(), value = inSum, color = Income, dot = Income)
        VerticalSep()
        TotalCell(label = stringResource(R.string.ledger_total_out).uppercase(), value = outSum, color = MaterialTheme.colorScheme.onSurface, dot = Expense)
        VerticalSep()
        TotalCell(label = stringResource(R.string.ledger_total_net).uppercase(), value = net, color = if (net < 0) Expense else MaterialTheme.colorScheme.onSurface, dot = null, sign = true)
    }
}

@Composable
private fun TotalCell(label: String, value: Double, color: Color, dot: Color?, sign: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = EyebrowStyle, color = Text3)
        }
        Spacer(Modifier.height(4.dp))
        val rendered = if (sign && value >= 0) "+" + IndianNumberFormat.formatCompact(value)
                       else IndianNumberFormat.formatCompact(value)
        Text(
            text = rendered,
            color = color,
            fontFamily = InstrumentSerif,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        )
    }
}

@Composable
private fun VerticalSep() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

// ───────────────────────────── Search + Filters ──────────────────────────────

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.transactions_search_placeholder), color = Text3) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Text3) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = null, tint = Text3)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(50.dp),
    )
}

@Composable
private fun ActiveFiltersStrip(
    filter: TransactionsFilter,
    accounts: List<Account>,
    cards: List<Card>,
    categories: List<Category>,
    tags: List<Tag>,
    queryActive: Boolean,
    onRemoveRange: () -> Unit,
    onRemoveType: () -> Unit,
    onRemoveAccount: () -> Unit,
    onRemoveCard: () -> Unit,
    onRemoveCategory: () -> Unit,
    onRemoveTag: () -> Unit,
    onClearAll: () -> Unit,
) {
    val hasActive = filter != TransactionsFilter() || queryActive
    if (!hasActive) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (filter.range != TimeRange.ALL_TIME) {
            val label = if (filter.range == TimeRange.CUSTOM &&
                filter.customDateStart != null && filter.customDateEnd != null
            ) {
                "${formatChipDate(filter.customDateStart)} – ${formatChipDate(filter.customDateEnd)}"
            } else {
                rangeLabel(filter.range)
            }
            DismissChip(label = label, onDismiss = onRemoveRange)
        }
        if (filter.typeFilter != null) {
            DismissChip(label = filter.typeFilter.displayLabel(), onDismiss = onRemoveType)
        }
        if (filter.accountId != null) {
            val name = accounts.firstOrNull { it.id == filter.accountId }?.name ?: filter.accountId
            DismissChip(label = name, onDismiss = onRemoveAccount)
        }
        if (filter.cardId != null) {
            val name = cards.firstOrNull { it.id == filter.cardId }?.name
                ?: stringResource(R.string.transactions_filter_card)
            DismissChip(label = name, onDismiss = onRemoveCard)
        }
        if (filter.categoryId != null) {
            val name = categories.firstOrNull { it.id == filter.categoryId }?.name
                ?: stringResource(R.string.transactions_filter_category)
            DismissChip(label = name, onDismiss = onRemoveCategory)
        }
        if (filter.tagId != null) {
            val name = tags.firstOrNull { it.id == filter.tagId }?.name ?: "Tag"
            DismissChip(label = name, onDismiss = onRemoveTag)
        }
        TextButton(
            onClick = onClearAll,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
        ) {
            Text(stringResource(R.string.transactions_filter_clear_all), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DismissChip(label: String, onDismiss: () -> Unit) {
    ElevatedFilterChip(
        selected = true,
        onClick = onDismiss,
        label = { Text(label) },
        trailingIcon = {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
        },
    )
}

private fun formatChipDate(millis: Long): String {
    val d = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${d.dayOfMonth} ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)}"
}

@Composable
internal fun rangeLabel(range: TimeRange): String = when (range) {
    TimeRange.TODAY -> stringResource(R.string.dashboard_section_recent_today)
    TimeRange.THIS_WEEK -> stringResource(R.string.dashboard_section_recent_week)
    TimeRange.THIS_MONTH -> stringResource(R.string.dashboard_section_recent_month)
    TimeRange.ALL_TIME -> stringResource(R.string.transactions_filter_date) + ": " +
        stringResource(R.string.transactions_filter_any)
    TimeRange.CUSTOM -> stringResource(R.string.ledger_range_custom)
}

@Composable
private fun rangeDisplay(range: TimeRange): String = when (range) {
    TimeRange.TODAY -> stringResource(R.string.ledger_range_today)
    TimeRange.THIS_WEEK -> stringResource(R.string.ledger_range_week)
    TimeRange.THIS_MONTH -> stringResource(R.string.ledger_range_month)
    TimeRange.ALL_TIME -> stringResource(R.string.ledger_range_all)
    TimeRange.CUSTOM -> stringResource(R.string.ledger_range_custom)
}


// ───────────────────────────── Day Header + Rows ─────────────────────────────

@Composable
private fun DayHeader(label: String, sum: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label.uppercase(), style = EyebrowStyle, color = Text3, modifier = Modifier.weight(1f))
        Text(
            text = (if (sum >= 0) "+" else "") + IndianNumberFormat.format(sum),
            color = Text3,
            fontFamily = IbmPlexMono,
            fontSize = 11.sp,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    txn: Transaction,
    category: Category?,
    selected: Boolean,
    selectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
    val isIncome = txn.type.isIncomeLike()
    val avatarColor = category?.let { Color(it.color) }
    // Prefer the category-specific icon; fall back to type glyph.
    val rowIcon = category?.icon
        ?.let { com.subramanya.artha.utils.MaterialIcons.resolve(it) }
        ?: iconForType(txn.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            // One focusable element for TalkBack (description + type + amount), and announce the
            // selected state in selection mode (it's otherwise conveyed only by background colour).
            .semantics(mergeDescendants = true) { if (selectionMode) this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    avatarColor ?: if (isIncome) incomeSoftFill() else expenseSoftFill()
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = rowIcon,
                contentDescription = null,
                tint = if (avatarColor != null) Color.White
                       else if (isIncome) Income else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description.ifBlank { txn.type.displayLabel() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            MonoMeta(text = buildString {
                val catName = category?.name
                if (!catName.isNullOrBlank()) { append(catName); append(" · ") }
                append(DateFormatter.shortDate(txn.date))
            })
        }
        Text(
            text = signedAmount(txn),
            color = if (isIncome) Income else MaterialTheme.colorScheme.onSurface,
            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// ───────────────────────────── Selection top bar ─────────────────────────────

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

// ───────────────────────────── helpers ───────────────────────────────────────

@Composable
internal fun TransactionType.displayLabel(): String = when (this) {
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
internal fun TransactionSort.displayLabel(): String = when (this) {
    TransactionSort.DATE_DESC -> stringResource(R.string.transactions_sort_date_desc)
    TransactionSort.DATE_ASC -> stringResource(R.string.transactions_sort_date_asc)
    TransactionSort.AMOUNT_DESC -> stringResource(R.string.transactions_sort_amount_desc)
    TransactionSort.AMOUNT_ASC -> stringResource(R.string.transactions_sort_amount_asc)
}

private fun iconForType(type: TransactionType) = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    TransactionType.INVESTMENT_BUY, TransactionType.INVESTMENT_SELL -> Icons.AutoMirrored.Filled.ShowChart
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED ->
        Icons.AutoMirrored.Filled.TrendingUp
    TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT ->
        Icons.AutoMirrored.Filled.TrendingDown
    TransactionType.ADJUSTMENT -> Icons.AutoMirrored.Filled.ReceiptLong
    else -> Icons.Filled.AccountBalance
}

private fun TransactionType.isIncomeLike() = this in setOf(
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
)

private fun signedAmount(txn: Transaction): String {
    val abs = IndianNumberFormat.format(txn.amount)
    return when {
        txn.type.isIncomeLike() -> "+$abs"
        txn.type == TransactionType.EXPENSE ||
            txn.type == TransactionType.LOAN_GIVEN ||
            txn.type == TransactionType.GIFT_SENT -> "−$abs"
        else -> abs
    }
}
