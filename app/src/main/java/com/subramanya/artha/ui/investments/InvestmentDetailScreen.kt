package com.subramanya.artha.ui.investments

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(
    investmentId: String,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InvestmentDetailViewModel = viewModel(
        factory = InvestmentDetailViewModelFactory(
            investmentId = investmentId,
            investmentRepository = app.investmentRepository,
            transactionRepository = app.transactionRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing: Investment? by remember { mutableStateOf(null) }

    Surface(
        color = com.subramanya.artha.ui.theme.Surface1,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val inv = state.investment
            com.subramanya.artha.ui.common.InlineTopBar(
                title = inv?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (inv != null) {
                        IconButton(onClick = { editing = inv }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.account_detail_action_edit),
                                tint = com.subramanya.artha.ui.theme.Text2,
                            )
                        }
                        if (inv.isArchived) {
                            IconButton(onClick = { vm.restore(onRestored = onBack) }) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = stringResource(R.string.account_detail_action_restore),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        } else {
                            IconButton(onClick = { vm.archive(onArchived = onBack) }) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = stringResource(R.string.account_detail_action_archive),
                                    tint = com.subramanya.artha.ui.theme.Text2,
                                )
                            }
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.account_action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
            if (inv == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
                return@Column
            }
            Column(modifier = Modifier.fillMaxSize()) {
                HeroBlock(
                    investment = inv,
                    invested = state.investedAmount,
                    gain = state.absoluteGain,
                    pctGain = state.percentGain,
                )

                MetaBlock(investment = inv)

                Text(
                    text = stringResource(R.string.investment_detail_txns_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 4.dp),
                )

                if (state.transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = stringResource(R.string.investment_detail_txns_empty),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.transactions, key = { it.id }) { txn ->
                            TransactionRow(txn = txn, onClick = { onOpenTransaction(txn.id) })
                        }
                    }
                }
            }
        }
    }

    val editingNow = editing
    if (editingNow != null) {
        InvestmentFormSheet(
            editing = editingNow,
            onDismiss = { editing = null },
        )
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = { Text(stringResource(R.string.investment_delete_confirm_title)) },
            text = { Text(stringResource(R.string.investment_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { vm.confirmDelete(onDeleted = onBack) }) {
                    Text(
                        text = stringResource(R.string.investment_delete_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDeleteConfirm) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

// ---------------- pieces ----------------

@Composable
private fun HeroBlock(investment: Investment, invested: Double, gain: Double, pctGain: Double) {
    val positive = gain >= 0.0
    val gainColor =
        if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(investment.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Savings, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = investment.type.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    investment.institution?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.investments_hero_current),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = IndianNumberFormat.format(investment.currentValue),
                style = ArthaAmountStyles.display,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (positive) Icons.AutoMirrored.Filled.TrendingUp
                    else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = gainColor,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = IndianNumberFormat.format(gain),
                    style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
                    color = gainColor,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (pctGain.isNaN()) "—" else "%+.2f%%".format(pctGain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = gainColor,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.investments_hero_invested) +
                    ": " + IndianNumberFormat.format(invested),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MetaBlock(investment: Investment) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        MetaRow(
            label = stringResource(R.string.investment_detail_meta_start),
            value = DateFormatter.longDate(investment.startDate),
        )
        investment.maturityDate?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_maturity),
                value = DateFormatter.longDate(it),
            )
        }
        investment.units?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_units),
                value = "%.4f".format(it).trimEnd('0').trimEnd('.'),
            )
        }
        investment.nav?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_nav),
                value = IndianNumberFormat.format(it),
            )
        }
        investment.taxSection?.takeIf { it.isNotBlank() }?.let {
            MetaRow(
                label = stringResource(R.string.investment_detail_meta_tax_section),
                value = it,
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(txn.description.ifBlank { txn.type.name }, maxLines = 1) },
        supportingContent = {
            Text(
                text = DateFormatter.shortDate(txn.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Text(
                text = IndianNumberFormat.format(txn.amount),
                style = ArthaAmountStyles.body.copy(fontWeight = FontWeight.SemiBold),
            )
        },
    )
}

@Composable
private fun InvestmentType.displayName(): String = when (this) {
    InvestmentType.FD -> stringResource(R.string.investment_type_fd)
    InvestmentType.RD -> stringResource(R.string.investment_type_rd)
    InvestmentType.SIP -> stringResource(R.string.investment_type_sip)
    InvestmentType.MUTUAL_FUND -> stringResource(R.string.investment_type_mutual_fund)
    InvestmentType.EQUITY -> stringResource(R.string.investment_type_equity)
    InvestmentType.GOLD_PHYSICAL -> stringResource(R.string.investment_type_gold_physical)
    InvestmentType.GOLD_DIGITAL -> stringResource(R.string.investment_type_gold_digital)
    InvestmentType.BONDS -> stringResource(R.string.investment_type_bonds)
    InvestmentType.PPF -> stringResource(R.string.investment_type_ppf)
    InvestmentType.EPF -> stringResource(R.string.investment_type_epf)
    InvestmentType.NPS -> stringResource(R.string.investment_type_nps)
    InvestmentType.ULIP -> stringResource(R.string.investment_type_ulip)
    InvestmentType.OTHER -> stringResource(R.string.investment_type_other)
}
