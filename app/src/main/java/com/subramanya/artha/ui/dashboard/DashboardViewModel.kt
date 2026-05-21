package com.subramanya.artha.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.MonthlyAggregator
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.TransactionRepository
import kotlinx.datetime.Clock
import com.subramanya.artha.utils.TimeRange
import com.subramanya.artha.utils.thisCalendarMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
) : ViewModel() {

    /** User-controlled chip on the Recent strip: Today / Week / Month. */
    private val recentRange = MutableStateFlow(TimeRange.TODAY)

    /** Cheap secondary bag — combined with the core flow inside [state] to keep the
     *  primary combine() under the 5-arg ceiling without nesting two layers. */
    private val phase2Bag = combine(
        investmentRepository.observeActive(),
        insuranceRepository.observeDueWithin(weekFromNow()),
    ) { investments, duePolicies ->
        Phase2Bag(
            investmentTotalValue = investments.sumOf { it.currentValue },
            premiumsDueThisWeek = duePolicies,
        )
    }

    val state: StateFlow<DashboardUiState> = combine(
        combine(
            accountRepository.observeActiveWithBalances(),
            cardRepository.observeActiveWithBalances(),
            monthlyTotalsFlow(),
            recentTransactionsFlow(),
            recentRange,
        ) { accounts, cards, monthly, recent, range ->
            CoreBag(accounts, cards, monthly, recent, range)
        },
        phase2Bag,
    ) { core, p2 ->
        // Net position = sum of bank/cash balances minus credit-card outstanding
        // plus current investment value. Liquid + paper wealth on one number.
        val accountSum = core.accounts.sumOf { it.currentBalance }
        val cardOutstandingSum = core.cards.sumOf { it.currentOutstanding }
        val netPosition = accountSum - cardOutstandingSum + p2.investmentTotalValue

        DashboardUiState(
            isLoading = false,
            netPosition = netPosition,
            accountCount = core.accounts.size,
            cardCount = core.cards.size,
            monthlyTotals = core.monthly,
            accounts = core.accounts,
            cards = core.cards,
            recentRange = core.range,
            recentTransactions = core.recent,
            premiumsDueThisWeek = p2.premiumsDueThisWeek,
            investmentTotalValue = p2.investmentTotalValue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private data class CoreBag(
        val accounts: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        val cards: List<com.subramanya.artha.domain.model.CardWithBalance>,
        val monthly: com.subramanya.artha.data.balance.MonthlyTotals,
        val recent: List<com.subramanya.artha.domain.model.Transaction>,
        val range: TimeRange,
    )
    private data class Phase2Bag(
        val investmentTotalValue: Double,
        val premiumsDueThisWeek: List<com.subramanya.artha.domain.model.Insurance>,
    )

    private fun weekFromNow(): Long =
        Clock.System.now().toEpochMilliseconds() + WEEK_MILLIS

    fun onRecentRangeChanged(range: TimeRange) {
        recentRange.update { range }
    }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun recentTransactionsFlow() = recentRange.flatMapLatest { range ->
        val window = range.toRange()
        transactionRepository.observeBetween(window.startMillis, window.endMillis)
            .map { list -> list.take(RECENT_TRANSACTION_LIMIT) }
    }

    private companion object {
        private const val RECENT_TRANSACTION_LIMIT: Int = 10
        private const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

class DashboardViewModelFactory(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val investmentRepository: InvestmentRepository,
    private val insuranceRepository: InsuranceRepository,
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
        ) as T
    }
}
