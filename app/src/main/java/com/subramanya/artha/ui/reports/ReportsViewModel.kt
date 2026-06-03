package com.subramanya.artha.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.InvestmentRepository
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

/** Holder folding the three investment/category sources into one combine slot. */
private data class InvestmentsAndCategories(
    val investments: List<com.subramanya.artha.domain.model.Investment>,
    val categories: List<com.subramanya.artha.domain.model.Category>,
    val valuesById: Map<String, Double>,
)

data class CategorySlice(val categoryId: String, val displayName: String, val total: Double)
data class MerchantRow(val name: String, val total: Double, val count: Int)
data class TaxSectionRow(val section: String, val used: Double, val limit: Double?)

data class ReportsUiState(
    val range: ReportRange = ReportRange.THIS_MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    /** Sums per category in window — sorted desc by total. */
    val spendingByCategory: List<CategorySlice> = emptyList(),
    val spendingByPaymentApp: List<CategorySlice> = emptyList(),
    val topMerchants: List<MerchantRow> = emptyList(),
    /** Tax-section buckets driven by Investment.taxSection + Transaction.taxSection. */
    val taxSections: List<TaxSectionRow> = emptyList(),
    val netWorth: Double = 0.0,
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val investmentRepository: InvestmentRepository,
    private val categoryRepository: CategoryRepository,
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
        ) { invs, cats, valuesById -> InvestmentsAndCategories(invs, cats, valuesById) },
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

        val byApp = inWindow
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.paymentApp }
            .map { (app, list) ->
                CategorySlice(
                    categoryId = app.name,
                    displayName = app.label(),
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
        val netWorth = accounts.sumOf { it.currentBalance } -
            cards.sumOf { it.currentOutstanding } +
            investments.sumOf { investmentValuesById[it.id] ?: it.currentValue }

        ReportsUiState(
            range = currentRange,
            totalIncome = income,
            totalExpense = expense,
            spendingByCategory = byCategory,
            spendingByPaymentApp = byApp,
            topMerchants = merchants,
            taxSections = taxSections,
            netWorth = netWorth,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun onRangeChanged(r: ReportRange) = range.update { r }

    private fun TransactionType.isIncomeish(): Boolean = this == TransactionType.INCOME ||
        this == TransactionType.INTEREST || this == TransactionType.CASHBACK ||
        this == TransactionType.REFUND

    private fun PaymentApp.label(): String = when (this) {
        PaymentApp.GPAY -> "GPay"
        PaymentApp.PHONEPE -> "PhonePe"
        PaymentApp.PAYTM -> "Paytm"
        PaymentApp.CRED -> "CRED"
        PaymentApp.BHIM -> "BHIM"
        PaymentApp.BANK_APP -> "Bank app"
        PaymentApp.CARD_SWIPE -> "Card swipe"
        PaymentApp.CASH -> "Cash"
        PaymentApp.NETBANKING -> "Netbanking"
        PaymentApp.OTHER -> "Other"
    }

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
        ) as T
    }
}
