package com.subramanya.artha.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.MonthlyAggregator
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.TransactionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.subramanya.artha.utils.thisCalendarMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    /** Cheap secondary bag — combined with the core flow inside [state] so the two
     *  bags stay under combine()'s 5-argument arity without an extra nesting layer. */
    private val phase2Bag = combine(
        investmentRepository.observeActive(),
        investmentRepository.observeValuesByInvestmentId(),
        insuranceRepository.observeDueWithin(weekFromNow()),
        categoryRepository.observeAll(),
    ) { investments, valuesById, duePolicies, categories ->
        Phase2Bag(
            // Active-only scope preserved: sum each active investment's COMPUTED value
            // (DERIVED rows reflect contributions + interest, not the stale currentValue).
            investmentTotalValue = investments.sumOf { valuesById[it.id] ?: it.currentValue },
            premiumsDueThisWeek = duePolicies,
            // id -> Category so the recent-activity rows can show each txn's real
            // category icon, colour and name (not a slug derived from the id).
            categoriesById = categories.associateBy { it.id },
        )
    }

    val state: StateFlow<DashboardUiState> = combine(
        combine(
            accountRepository.observeActiveWithBalances(),
            cardRepository.observeActiveWithBalances(),
            monthlyTotalsFlow(),
        ) { accounts, cards, monthly ->
            CoreBag(accounts, cards, monthly)
        },
        combine(phase2Bag, transactionRepository.observeAll()) { p2, allTxns ->
            Phase2BagWithTxns(p2, allTxns)
        },
    ) { core, p2withTxns ->
        // Net position = sum of bank/cash balances minus credit-card outstanding
        // plus current investment value. Liquid + paper wealth on one number.
        val accountSum = core.accounts.sumOf { it.currentBalance }
        val cardOutstandingSum = core.cards.sumOf { it.currentOutstanding }
        val investmentValue = p2withTxns.bag.investmentTotalValue
        val netPosition = accountSum - cardOutstandingSum + investmentValue

        // Recent activity = the most recent transactions regardless of date, so the
        // strip is never empty while there's any history (observeAll is date DESC).
        val recent = p2withTxns.transactions.take(RECENT_TRANSACTION_LIMIT)

        // Convert the transaction log to entities once — reused by both the
        // sparkline and the month-start baseline below.
        val entities = p2withTxns.transactions.map { it.toEntity() }

        // 30-day daily-net spark: replay account+card balances from history.
        // Cheap approximation — assumes investments are flat in the window (we
        // don't track historical NAV). Good enough to show shape.
        val spark = buildNetPositionSpark(
            accounts = core.accounts,
            cards = core.cards,
            investmentValue = investmentValue,
            entities = entities,
            days = SPARK_DAYS,
        )

        // True change in net position this calendar month = net now minus net at
        // the instant before the month began. The current investment value sits on
        // both sides, so it cancels (consistent with the sparkline's flat-investment
        // assumption), leaving the realized liquid + card movement. This also makes
        // the hero's "+x.x%" denominator (netPosition - netChange) equal the actual
        // start-of-month net position rather than a fabricated number.
        val month = thisCalendarMonth()
        val netAtMonthStart = computeNetPositionAsOf(
            cutoffMillis = month.startMillis - 1,
            accounts = core.accounts,
            cards = core.cards,
            investmentValue = investmentValue,
            entities = entities,
        )
        val netChange = netPosition - netAtMonthStart

        // "Where it went": this month's EXPENSE transactions rolled up to the MAIN
        // (parent) category — sub-categories aggregate into their parent — top spenders
        // first. Uncategorised expenses bucket under a null category.
        val categoriesById = p2withTxns.bag.categoriesById
        val topSpending = p2withTxns.transactions
            .filter {
                it.type == com.subramanya.artha.data.entity.enums.TransactionType.EXPENSE &&
                    it.date in month.startMillis..month.endMillis
            }
            .groupBy { txn ->
                val cat = txn.categoryId?.let { categoriesById[it] }
                // Roll a sub-category up to its parent; a top-level category stays itself.
                (cat?.parentId?.let { categoriesById[it] } ?: cat)?.id
            }
            .map { (rootId, txns) ->
                com.subramanya.artha.ui.dashboard.CategorySpend(
                    category = rootId?.let { categoriesById[it] },
                    amount = txns.sumOf { it.amount },
                )
            }
            .sortedByDescending { it.amount }
            .take(SPENDING_TOP_N)

        DashboardUiState(
            isLoading = false,
            netPosition = netPosition,
            accountCount = core.accounts.size,
            cardCount = core.cards.size,
            monthlyTotals = core.monthly,
            accounts = core.accounts,
            cards = core.cards,
            recentTransactions = recent,
            premiumsDueThisWeek = p2withTxns.bag.premiumsDueThisWeek,
            investmentTotalValue = p2withTxns.bag.investmentTotalValue,
            netPositionSpark = spark,
            netChangeThisMonth = netChange,
            categoriesById = p2withTxns.bag.categoriesById,
            topSpending = topSpending,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** End-of-day net position for the last [days] days, oldest first. Reuses the
     *  pinned single-pass batch [BalanceCalculator] methods so the sparkline can never
     *  drift from the displayed Net Position number. Each day's prefix is obtained by
     *  advancing an index over the date-sorted log (so the log is walked once overall)
     *  rather than re-filtering and re-scanning per account, per card, per day. */
    private fun buildNetPositionSpark(
        accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        investmentValue: Double,
        entities: List<com.subramanya.artha.data.entity.TransactionEntity>,
        days: Int,
    ): List<Double> {
        if (accounts.isEmpty() && cards.isEmpty() && investmentValue == 0.0) return emptyList()
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        val today = kotlinx.datetime.Instant.fromEpochMilliseconds(
            Clock.System.now().toEpochMilliseconds(),
        ).toLocalDateTime(tz).date
        val openingById = accounts.associate { it.account.id to it.account.openingBalance }
        val cardIds = cards.map { it.card.id }
        val sorted = entities.sortedBy { it.date }
        val points = ArrayList<Double>(days)
        var idx = 0
        for (daysAgo in (days - 1) downTo 0) {
            val day = today.minus(daysAgo, kotlinx.datetime.DateTimeUnit.DAY)
            val cutoff = day.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
                .atStartOfDayIn(tz).toEpochMilliseconds() - 1
            while (idx < sorted.size && sorted[idx].date <= cutoff) idx++
            points.add(netPositionOf(sorted.subList(0, idx), openingById, cardIds, investmentValue))
        }
        // Hide a flat-zero line — it adds no signal.
        if (points.all { it == 0.0 }) return emptyList()
        return points
    }

    /** Net position (liquid - card outstanding + [investmentValue]) as of [cutoffMillis]. */
    private fun computeNetPositionAsOf(
        cutoffMillis: Long,
        accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        investmentValue: Double,
        entities: List<com.subramanya.artha.data.entity.TransactionEntity>,
    ): Double = netPositionOf(
        transactions = entities.filter { it.date <= cutoffMillis },
        openingById = accounts.associate { it.account.id to it.account.openingBalance },
        cardIds = cards.map { it.card.id },
        investmentValue = investmentValue,
    )

    /** Net position over a transaction prefix via the pinned single-pass batch
     *  [BalanceCalculator] methods (O(transactions + accounts/cards)). Centralised so the
     *  spark, the month-start baseline and the hero number all use identical math. */
    private fun netPositionOf(
        transactions: List<com.subramanya.artha.data.entity.TransactionEntity>,
        openingById: Map<String, Double>,
        cardIds: Collection<String>,
        investmentValue: Double,
    ): Double {
        val acctSum = com.subramanya.artha.data.balance.BalanceCalculator
            .computeAccountBalances(openingById, transactions).values.sum()
        val cardSum = com.subramanya.artha.data.balance.BalanceCalculator
            .computeCardOutstandings(cardIds, transactions).values.sum()
        return acctSum - cardSum + investmentValue
    }

    private data class CoreBag(
        val accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        val cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        val monthly: com.subramanya.artha.data.balance.MonthlyTotals,
    )
    private data class Phase2Bag(
        val investmentTotalValue: Double,
        val premiumsDueThisWeek: List<com.subramanya.artha.domain.model.Insurance>,
        val categoriesById: Map<String, com.subramanya.artha.domain.model.Category>,
    )
    private data class Phase2BagWithTxns(
        val bag: Phase2Bag,
        val transactions: List<com.subramanya.artha.domain.model.Transaction>,
    )

    private fun weekFromNow(): Long =
        Clock.System.now().toEpochMilliseconds() + WEEK_MILLIS

    private fun monthlyTotalsFlow() = transactionRepository.observeAll()
        .map { domainList ->
            val month = thisCalendarMonth()
            // Map back to entity so MonthlyAggregator can stay pure-data; the round-trip
            // is cheap and avoids leaking enum sets into the domain layer.
            val inMonth = domainList
                .filter { it.date in month.startMillis..month.endMillis }
                .map { it.toEntity() }
            MonthlyAggregator.aggregate(inMonth)
        }

    private companion object {
        private const val RECENT_TRANSACTION_LIMIT: Int = 10
        private const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
        private const val SPARK_DAYS: Int = 30
        private const val SPENDING_TOP_N: Int = 5
    }
}

class DashboardViewModelFactory(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return DashboardViewModel(
            accountRepository,
            cardRepository,
            transactionRepository,
            investmentRepository,
            insuranceRepository,
            categoryRepository,
        ) as T
    }
}
