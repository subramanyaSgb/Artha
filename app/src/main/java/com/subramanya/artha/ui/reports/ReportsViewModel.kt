package com.subramanya.artha.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.entity.TransactionEntity
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.PaymentAppRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class ReportRange { THIS_MONTH, LAST_MONTH, FISCAL_YEAR }

/** Holder folding the investment/category/payment-app sources into one combine slot. */
private data class InvestmentsAndCategories(
    val investments: List<com.subramanya.artha.domain.model.Investment>,
    val categories: List<com.subramanya.artha.domain.model.Category>,
    val valuesById: Map<String, Double>,
    /** payment-app catalogue id → display label, for the "spending by app" slice. */
    val paymentAppLabels: Map<String, String>,
)

data class CategorySlice(val categoryId: String, val displayName: String, val total: Double)
data class MerchantRow(val name: String, val total: Double, val count: Int)
data class TaxSectionRow(val section: String, val used: Double, val limit: Double?)

/** One month's income vs expense for the trailing-months bar chart. */
data class MonthlyInOut(val label: String, val income: Double, val expense: Double)

data class ReportsUiState(
    val range: ReportRange = ReportRange.THIS_MONTH,
    val isLoading: Boolean = true,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    /** Sums per category in window — sorted desc by total. */
    val spendingByCategory: List<CategorySlice> = emptyList(),
    val spendingByPaymentApp: List<CategorySlice> = emptyList(),
    val topMerchants: List<MerchantRow> = emptyList(),
    /** Tax-section buckets driven by Investment.taxSection + Transaction.taxSection. */
    val taxSections: List<TaxSectionRow> = emptyList(),
    val netWorth: Double = 0.0,
    /** End-of-period net worth sampled across the selected window (oldest first). */
    val netWorthTrend: List<Double> = emptyList(),
    /** Income vs expense for the trailing 6 calendar months (oldest first). */
    val incomeExpenseMonths: List<MonthlyInOut> = emptyList(),
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val investmentRepository: InvestmentRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentAppRepository: PaymentAppRepository,
) : ViewModel() {

    private val range = MutableStateFlow(ReportRange.THIS_MONTH)

    val state: StateFlow<ReportsUiState> = combine(
        transactionRepository.observeAll(),
        accountRepository.observeActiveWithBalances(),
        cardRepository.observeActiveWithBalances(),
        // 4th source folds investments + categories + computed-value map into a
        // holder so the outer combine stays inside the 5-arg arity cap.
        combine(
            investmentRepository.observeActive(),
            categoryRepository.observeAll(),
            investmentRepository.observeValuesByInvestmentId(),
            paymentAppRepository.observeAll(),
        ) { invs, cats, valuesById, apps ->
            InvestmentsAndCategories(invs, cats, valuesById, apps.associate { it.id to it.label })
        },
        range,
    ) { txns, accounts, cards, invCat, currentRange ->
        val investments = invCat.investments
        val categories = invCat.categories
        val investmentValuesById = invCat.valuesById
        val window = windowFor(currentRange)
        val inWindow = txns.filter { it.date in window.first..window.second }

        val income = inWindow.filter { it.type.isIncomeish() }.sumOf { it.amount }
        val expense = inWindow.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Build an id → friendly name lookup so the by-category slice shows
        // "Food & Drink" instead of "cat_food_drink".
        val categoryNameById = categories.associateBy({ it.id }, { it.name })

        val byCategory = inWindow
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId ?: "uncategorised" }
            .map { (cid, list) ->
                CategorySlice(
                    categoryId = cid,
                    displayName = categoryNameById[cid] ?: "Uncategorised",
                    total = list.sumOf { it.amount },
                )
            }
            .sortedByDescending { it.total }
            .take(10)

        val paymentAppLabels = invCat.paymentAppLabels
        val byApp = inWindow
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.paymentApp }
            .map { (appId, list) ->
                CategorySlice(
                    categoryId = appId,
                    // Resolve catalogue id -> label; fall back to the raw id for a hidden/removed app.
                    displayName = paymentAppLabels[appId] ?: appId,
                    total = list.sumOf { it.amount },
                )
            }
            .sortedByDescending { it.total }

        val merchants = inWindow
            .filter { it.type == TransactionType.EXPENSE && it.description.isNotBlank() }
            .groupBy { merchantKey(it.description) }
            .map { (key, list) ->
                MerchantRow(name = key, total = list.sumOf { it.amount }, count = list.size)
            }
            .sortedByDescending { it.total }
            .take(10)

        val taxSections = buildTaxSections(inWindow, investments, investmentValuesById)

        // Active-only scope preserved (observeActive); each investment contributes its
        // COMPUTED per-mode value rather than the stale raw currentValue.
        val investmentValue = investments.sumOf { investmentValuesById[it.id] ?: it.currentValue }
        val netWorth = accounts.sumOf { it.currentBalance } -
            cards.sumOf { it.currentOutstanding } +
            investmentValue

        // Net-worth trend + monthly in/out both replay the full log via the pinned
        // batch BalanceCalculator (same math as the Dashboard spark), off the main thread.
        val entities = txns.map { it.toEntity() }
        val netWorthTrend = buildNetWorthTrend(window, accounts, cards, investmentValue, entities)
        val incomeExpenseMonths = buildMonthlyInOut(txns)

        ReportsUiState(
            range = currentRange,
            isLoading = false,
            totalIncome = income,
            totalExpense = expense,
            spendingByCategory = byCategory,
            spendingByPaymentApp = byApp,
            topMerchants = merchants,
            taxSections = taxSections,
            netWorth = netWorth,
            netWorthTrend = netWorthTrend,
            incomeExpenseMonths = incomeExpenseMonths,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun onRangeChanged(r: ReportRange) = range.update { r }

    private fun TransactionType.isIncomeish(): Boolean = this == TransactionType.INCOME ||
        this == TransactionType.INTEREST || this == TransactionType.CASHBACK ||
        this == TransactionType.REFUND

    /** Reduce a transaction description down to a recognisable merchant key.
     *  Crude but effective — drops UPI prefixes, ref numbers, ids. */
    private fun merchantKey(description: String): String {
        val cleaned = description
            .replace(Regex("\\b\\d{6,}\\b"), "")
            .replace(Regex("(?i)upiout|upi/|neft/|imps/|rtgs/|pos/"), "")
            .replace(Regex("[/_]+"), " ")
            .trim()
        return cleaned.take(40).ifBlank { "(no description)" }
    }

    private fun buildTaxSections(
        txns: List<Transaction>,
        investments: List<com.subramanya.artha.domain.model.Investment>,
        investmentValuesById: Map<String, Double>,
    ): List<TaxSectionRow> {
        // Aggregate by section across BOTH investments (taxSection field) and any
        // transactions explicitly tagged with a tax section (set by rules engine).
        val totals = mutableMapOf<String, Double>()
        investments.forEach { inv ->
            inv.taxSection?.takeIf { it.isNotBlank() }?.let {
                val value = investmentValuesById[inv.id] ?: inv.currentValue
                totals.merge(it.uppercase(), value) { a, b -> a + b }
            }
        }
        txns.forEach { txn ->
            txn.taxSection?.takeIf { it.isNotBlank() }?.let {
                if (txn.type == TransactionType.EXPENSE || txn.type == TransactionType.INVESTMENT_BUY) {
                    totals.merge(it.uppercase(), txn.amount) { a, b -> a + b }
                }
            }
        }
        return totals.entries
            .sortedByDescending { it.value }
            .map { TaxSectionRow(section = it.key, used = it.value, limit = limitFor(it.key)) }
    }

    /**
     * End-of-period net worth sampled at up to 24 evenly-spaced cutoffs across [window]
     * (clamped to now — no future points). Each point replays the FULL transaction prefix
     * via the pinned batch [BalanceCalculator], so the trend can't drift from the hero number.
     * Investments are held flat across the window (we don't track historical NAV) — same
     * approximation the Dashboard sparkline uses.
     */
    private fun buildNetWorthTrend(
        window: Pair<Long, Long>,
        accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        investmentValue: Double,
        entities: List<TransactionEntity>,
    ): List<Double> {
        if (accounts.isEmpty() && cards.isEmpty() && investmentValue == 0.0) return emptyList()
        val now = Clock.System.now().toEpochMilliseconds()
        val start = window.first
        val end = minOf(window.second, now)
        val step = (end - start) / 24
        if (step <= 0L) return emptyList()
        val openingById = accounts.associate { it.account.id to it.account.openingBalance }
        val cardIds = cards.map { it.card.id }
        val sorted = entities.sortedBy { it.date }
        val points = ArrayList<Double>(24)
        var idx = 0
        var cutoff = start + step
        while (cutoff <= end) {
            while (idx < sorted.size && sorted[idx].date <= cutoff) idx++
            points.add(netPositionOf(sorted.subList(0, idx), openingById, cardIds, investmentValue))
            cutoff += step
        }
        // A flat-zero line carries no signal.
        return if (points.all { it == 0.0 }) emptyList() else points
    }

    /** Net position (liquid − card outstanding + [investmentValue]) over a transaction prefix. */
    private fun netPositionOf(
        transactions: List<TransactionEntity>,
        openingById: Map<String, Double>,
        cardIds: Collection<String>,
        investmentValue: Double,
    ): Double {
        val acctSum = BalanceCalculator.computeAccountBalances(openingById, transactions).values.sum()
        val cardSum = BalanceCalculator.computeCardOutstandings(cardIds, transactions).values.sum()
        return acctSum - cardSum + investmentValue
    }

    /** Income vs expense for the trailing 6 calendar months (oldest first), regardless of the
     *  range picker — a single-month range would otherwise make a one-bar chart. */
    private fun buildMonthlyInOut(txns: List<Transaction>): List<MonthlyInOut> {
        val tz = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(tz).date
        val firstOfThisMonth = LocalDate(today.year, today.monthNumber, 1)
        val result = ArrayList<MonthlyInOut>(6)
        for (back in 5 downTo 0) {
            val monthStart = firstOfThisMonth.plus(-back, DateTimeUnit.MONTH)
            val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH)
            val startMs = monthStart.atStartOfDayIn(tz).toEpochMilliseconds()
            val endMs = monthEnd.atStartOfDayIn(tz).toEpochMilliseconds()
            val monthTxns = txns.filter { it.date in startMs until endMs }
            result.add(
                MonthlyInOut(
                    label = monthStart.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3),
                    income = monthTxns.filter { it.type.isIncomeish() }.sumOf { it.amount },
                    expense = monthTxns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                ),
            )
        }
        return result
    }

    private fun limitFor(section: String): Double? = when (section) {
        "80C" -> 150_000.0
        "80CCD(1B)" -> 50_000.0
        "80D" -> 25_000.0
        else -> null
    }

    private fun windowFor(r: ReportRange): Pair<Long, Long> {
        val tz = TimeZone.currentSystemDefault()
        val today = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(tz).date
        return when (r) {
            ReportRange.THIS_MONTH -> {
                val start = LocalDate(today.year, today.monthNumber, 1)
                val end = start.plus(1, DateTimeUnit.MONTH)
                start.atStartOfDayIn(tz).toEpochMilliseconds() to
                    (end.atStartOfDayIn(tz).toEpochMilliseconds() - 1)
            }
            ReportRange.LAST_MONTH -> {
                val firstOfThis = LocalDate(today.year, today.monthNumber, 1)
                val start = firstOfThis.plus(-1, DateTimeUnit.MONTH)
                val end = firstOfThis
                start.atStartOfDayIn(tz).toEpochMilliseconds() to
                    (end.atStartOfDayIn(tz).toEpochMilliseconds() - 1)
            }
            ReportRange.FISCAL_YEAR -> {
                // Indian FY: April 1 → March 31.
                val fyStartYear = if (today.monthNumber >= 4) today.year else today.year - 1
                val start = LocalDate(fyStartYear, 4, 1)
                val end = LocalDate(fyStartYear + 1, 4, 1)
                start.atStartOfDayIn(tz).toEpochMilliseconds() to
                    (end.atStartOfDayIn(tz).toEpochMilliseconds() - 1)
            }
        }
    }
}

class ReportsViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val investmentRepository: InvestmentRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentAppRepository: PaymentAppRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return ReportsViewModel(
            transactionRepository,
            accountRepository,
            cardRepository,
            investmentRepository,
            categoryRepository,
            paymentAppRepository,
        ) as T
    }
}
