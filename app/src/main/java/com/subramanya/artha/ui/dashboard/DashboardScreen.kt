package com.subramanya.artha.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.ui.transaction.AddTransactionSheet
import com.subramanya.artha.ui.transaction.AddTransactionViewModel
import com.subramanya.artha.ui.transaction.AddTransactionViewModelFactory
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.TimeRange

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
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

    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp), // leave room for the FAB
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                NetPositionHero(state = state)

                Spacer(modifier = Modifier.height(16.dp))
                MonthlyStrip(state = state)

                Spacer(modifier = Modifier.height(24.dp))
                AccountsRow(accounts = state.accounts)

                Spacer(modifier = Modifier.height(24.dp))
                CardsRow(cards = state.cards)

                Spacer(modifier = Modifier.height(24.dp))
                RecentSection(
                    range = state.recentRange,
                    transactions = state.recentTransactions,
                    onRangeChanged = vm::onRecentRangeChanged,
                )
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
private fun AccountsRow(accounts: List<AccountWithBalance>) {
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
                    AccountTile(withBalance)
                }
            }
        }
    }
}

@Composable
private fun AccountTile(withBalance: AccountWithBalance) {
    Card(modifier = Modifier.width(180.dp)) {
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
private fun CardsRow(cards: List<CardWithBalance>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_section_cards),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (cards.isEmpty()) {
            // Cards aren't a Phase-1 onboarding requirement; quiet empty-state.
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                cards.forEach { withBalance ->
                    CardTile(withBalance)
                }
            }
        }
    }
}

@Composable
private fun CardTile(withBalance: CardWithBalance) {
    Card(modifier = Modifier.width(200.dp)) {
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
            // "View all" deep-link to Transactions screen — wired in Session 10 polish.
            TextButton(onClick = { /* deferred */ }) {
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
                    TransactionRow(txn = txn)
                }
            }
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
private fun TransactionRow(txn: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    ExtendedFloatingActionButton(
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.dashboard_fab_add)) },
        onClick = {},
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
        },
    )
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
