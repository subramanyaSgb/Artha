package com.subramanya.artha.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.AccountWithBalance
import com.subramanya.artha.domain.model.CardWithBalance
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.common.RefreshableContent
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.TimeRange

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onOpenTransactions: () -> Unit = {},
    onOpenAccount: (String) -> Unit = {},
    onOpenCard: (String) -> Unit = {},
    onOpenTransaction: (String) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onAddCard: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication

    val vm: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            transactionRepository = app.transactionRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }

    // Section visibility — user toggles via Settings → Dashboard sections.
    val showMonthly by app.settingsPreferences.dashboardShowMonthly.collectAsStateWithLifecycle(initialValue = true)
    val showAccounts by app.settingsPreferences.dashboardShowAccounts.collectAsStateWithLifecycle(initialValue = true)
    val showCards by app.settingsPreferences.dashboardShowCards.collectAsStateWithLifecycle(initialValue = true)
    val showRecent by app.settingsPreferences.dashboardShowRecent.collectAsStateWithLifecycle(initialValue = true)

    Surface(modifier = modifier.fillMaxSize()) {
        RefreshableContent(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp), // leave room for the FAB
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                NetPositionHero(state = state)

                if (showMonthly) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthlyStrip(state = state)
                }

                if (showAccounts) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AccountsRow(
                        accounts = state.accounts,
                        onOpenAccount = onOpenAccount,
                        onAddAccount = onAddAccount,
                    )
                }

                if (showCards) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CardsRow(
                        cards = state.cards,
                        onOpenCard = onOpenCard,
                        onAddCard = onAddCard,
                    )
                }

                if (showRecent) {
                    Spacer(modifier = Modifier.height(24.dp))
                    RecentSection(
                        range = state.recentRange,
                        transactions = state.recentTransactions,
                        onRangeChanged = vm::onRecentRangeChanged,
                        onViewAll = onOpenTransactions,
                        onOpenTransaction = onOpenTransaction,
                    )
                }
            }

            FabRow(
                onTap = { showSheet = true },
                onLongPress = {
                    Toast.makeText(context, R.string.dashboard_fab_ai_toast, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    if (showSheet) {
        val txnVm: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModelFactory(
                accountRepository = app.accountRepository,
                cardRepository = app.cardRepository,
                categoryRepository = app.categoryRepository,
                personRepository = app.personRepository,
                tagRepository = app.tagRepository,
                transactionRepository = app.transactionRepository,
                settingsPreferences = app.settingsPreferences,
            ),
        )
        AddTransactionSheet(viewModel = txnVm, onDismiss = { showSheet = false })
    }
}

// ---------------- hero / monthly strip ----------------

@Composable
private fun NetPositionHero(state: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dashboard_net_position),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = IndianNumberFormat.format(state.netPosition),
                style = ArthaAmountStyles.display,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.dashboard_net_position_subtitle,
                    state.accountCount,
                    state.cardCount,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MonthlyStrip(state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoneyMiniCard(
            modifier = Modifier.weight(1f),
            labelRes = R.string.dashboard_this_month_income,
            amount = state.monthlyTotals.income,
            color = colorIncome(),
        )
        MoneyMiniCard(
            modifier = Modifier.weight(1f),
            labelRes = R.string.dashboard_this_month_expense,
            amount = state.monthlyTotals.expense,
            color = colorExpense(),
            negative = true,
        )
    }
}

@Composable
private fun MoneyMiniCard(
    modifier: Modifier,
    labelRes: Int,
    amount: Double,
    color: Color,
    negative: Boolean = false,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sign = if (negative) "−" else "+"
            Text(
                text = sign + IndianNumberFormat.format(amount),
                style = ArthaAmountStyles.title,
                color = color,
            )
        }
    }
}

// ---------------- horizontal rows ----------------

@Composable
private fun AccountsRow(
    accounts: List<AccountWithBalance>,
    onOpenAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_section_accounts),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (accounts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AccountBalance,
                title = stringResource(R.string.dashboard_accounts_empty),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                accounts.forEach { withBalance ->
                    AccountTile(withBalance, onClick = { onOpenAccount(withBalance.account.id) })
                }
                AddTile(onClick = onAddAccount)
            }
        }
    }
}

@Composable
private fun AccountTile(withBalance: AccountWithBalance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = withBalance.account.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            val subtitle = buildString {
                if (!withBalance.account.institution.isNullOrBlank()) {
                    append(withBalance.account.institution)
                }
                if (!withBalance.account.accountNumberLast4.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("••${withBalance.account.accountNumberLast4}")
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = IndianNumberFormat.format(withBalance.currentBalance),
                style = ArthaAmountStyles.title,
            )
        }
    }
}

@Composable
private fun CardsRow(
    cards: List<CardWithBalance>,
    onOpenCard: (String) -> Unit,
    onAddCard: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_section_cards),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (cards.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CreditCard,
                title = stringResource(R.string.dashboard_section_cards_empty),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                cards.forEach { withBalance ->
                    CardTile(withBalance, onClick = { onOpenCard(withBalance.card.id) })
                }
                AddTile(onClick = onAddCard)
            }
        }
    }
}

@Composable
private fun CardTile(withBalance: CardWithBalance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = withBalance.card.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = withBalance.card.network.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = IndianNumberFormat.format(withBalance.currentOutstanding),
                style = ArthaAmountStyles.title,
            )
        }
    }
}

// ---------------- recent transactions ----------------

@Composable
private fun RecentSection(
    range: TimeRange,
    transactions: List<Transaction>,
    onRangeChanged: (TimeRange) -> Unit,
    onViewAll: () -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_section_recent),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.dashboard_view_all))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RangeChip(range, TimeRange.TODAY, R.string.dashboard_section_recent_today, onRangeChanged)
            RangeChip(range, TimeRange.THIS_WEEK, R.string.dashboard_section_recent_week, onRangeChanged)
            RangeChip(range, TimeRange.THIS_MONTH, R.string.dashboard_section_recent_month, onRangeChanged)
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = stringResource(R.string.dashboard_recent_empty),
            )
        } else {
            Column {
                transactions.forEach { txn ->
                    TransactionRow(txn = txn, onClick = { onOpenTransaction(txn.id) })
                }
            }
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RangeChip(
    current: TimeRange,
    target: TimeRange,
    labelRes: Int,
    onClick: (TimeRange) -> Unit,
) {
    FilterChip(
        selected = current == target,
        onClick = { onClick(target) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconForType(txn.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = txn.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = signedAmount(txn),
            style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor(txn.type),
        )
    }
}

// ---------------- FAB ----------------

@Composable
private fun FabRow(onTap: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    // Compose pitfall: the FAB internally wires Modifier.clickable, which consumes
    // events on the default Main pass before any outer detector can see them. To layer
    // a long-press handler on top of the existing tap, we watch the Initial pass — the
    // parent sees raw events first. AwaitPointerEventScope.withTimeout (the scope-aware
    // overload — kotlinx.coroutines.withTimeout is not callable from this restricted
    // scope) throws PointerEventTimeoutCancellationException when the press is held
    // long enough; we treat that as a long-press, consume the down event so the FAB's
    // clickable does not also fire a tap on release.
    val haptic = LocalHapticFeedback.current
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                try {
                    withTimeout(longPressTimeoutMillis) {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    }
                    // Released before the timeout — let the FAB's own clickable handle the tap.
                } catch (_: PointerEventTimeoutCancellationException) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                    down.consume()
                }
            }
        },
    ) {
        ExtendedFloatingActionButton(
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.dashboard_fab_add)) },
            onClick = onTap,
        )
    }
}

// ---------------- helpers ----------------

@Composable private fun colorIncome(): Color = MaterialTheme.colorScheme.primary
@Composable private fun colorExpense(): Color = MaterialTheme.colorScheme.error

@Composable
private fun amountColor(type: TransactionType): Color = when (type) {
    TransactionType.INCOME, TransactionType.REFUND, TransactionType.CASHBACK,
    TransactionType.INTEREST, TransactionType.LOAN_RECEIVED, TransactionType.GIFT_RECEIVED,
    -> colorIncome()
    TransactionType.EXPENSE, TransactionType.LOAN_GIVEN, TransactionType.GIFT_SENT,
    -> colorExpense()
    else -> MaterialTheme.colorScheme.onSurface
}

private fun iconForType(type: TransactionType) = when (type) {
    TransactionType.CARD_PAYMENT -> Icons.Filled.CreditCard
    else -> Icons.Filled.AccountBalance
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
