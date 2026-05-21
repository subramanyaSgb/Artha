package com.subramanya.artha.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.IndianNumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            investmentRepository = app.investmentRepository,
            categoryRepository = app.categoryRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.reports_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.about_back))
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                RangePicker(state.range, vm::onRangeChanged)

                NetWorthCard(state.netWorth)

                IncomeExpenseCard(income = state.totalIncome, expense = state.totalExpense)

                SectionHeader(stringResource(R.string.reports_spending_by_category))
                if (state.spendingByCategory.isEmpty()) EmptyHint(stringResource(R.string.reports_empty_period))
                state.spendingByCategory.forEach { slice ->
                    SliceRow(label = slice.displayName, value = slice.total,
                        max = state.spendingByCategory.first().total)
                }

                SectionHeader(stringResource(R.string.reports_spending_by_payment_app))
                state.spendingByPaymentApp.forEach { slice ->
                    SliceRow(label = slice.displayName, value = slice.total,
                        max = state.spendingByPaymentApp.first().total)
                }

                SectionHeader(stringResource(R.string.reports_top_merchants))
                state.topMerchants.forEach { merchant ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(merchant.name, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false))
                        Text(
                            text = IndianNumberFormat.format(merchant.total) + " · ×${merchant.count}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                SectionHeader(stringResource(R.string.reports_tax_sections))
                if (state.taxSections.isEmpty()) EmptyHint(stringResource(R.string.reports_tax_empty))
                state.taxSections.forEach { row ->
                    TaxSectionCard(row)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RangePicker(current: ReportRange, onChange: (ReportRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = current == ReportRange.THIS_MONTH,
            onClick = { onChange(ReportRange.THIS_MONTH) },
            label = { Text(stringResource(R.string.reports_range_this_month)) },
        )
        FilterChip(
            selected = current == ReportRange.LAST_MONTH,
            onClick = { onChange(ReportRange.LAST_MONTH) },
            label = { Text(stringResource(R.string.reports_range_last_month)) },
        )
        FilterChip(
            selected = current == ReportRange.FISCAL_YEAR,
            onClick = { onChange(ReportRange.FISCAL_YEAR) },
            label = { Text(stringResource(R.string.reports_range_fy)) },
        )
    }
}

@Composable
private fun NetWorthCard(value: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.reports_net_worth),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                IndianNumberFormat.format(value),
                style = ArthaAmountStyles.display,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun IncomeExpenseCard(income: Double, expense: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.reports_income),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    IndianNumberFormat.format(income),
                    style = ArthaAmountStyles.title,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.reports_expense),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    IndianNumberFormat.format(expense),
                    style = ArthaAmountStyles.title,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SliceRow(label: String, value: Double, max: Double) {
    val ratio = if (max == 0.0) 0f else (value / max).toFloat()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                IndianNumberFormat.format(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TaxSectionCard(row: TaxSectionRow) {
    val ratio = if (row.limit == null || row.limit == 0.0) 0f else (row.used / row.limit).toFloat().coerceIn(0f, 1f)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Section ${row.section}", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = IndianNumberFormat.format(row.used) +
                        (row.limit?.let { " / " + IndianNumberFormat.format(it) } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (row.limit != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
