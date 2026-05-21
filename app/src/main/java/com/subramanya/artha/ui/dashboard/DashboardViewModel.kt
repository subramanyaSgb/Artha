package com.subramanya.artha.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.MonthlyAggregator
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CardRepository
import com.subramanya.artha.data.repository.TransactionRepository
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
) : ViewModel() {

    /** User-controlled chip on the Recent strip: Today / Week / Month. */
    private val recentRange = MutableStateFlow(TimeRange.TODAY)

    val state: StateFlow<DashboardUiState> = combine(
        accountRepository.observeActiveWithBalances(),
        cardRepository.observeActiveWithBalances(),
        monthlyTotalsFlow(),
        recentTransactionsFlow(),
        recentRange,
    ) { accounts, cards, monthly, recent, range ->
        // Net position = sum of bank/cash balances minus credit-card outstanding.
        // Investments will join this calculation in Phase 2.
        val accountSum = accounts.sumOf { it.currentBalance }
        val cardOutstandingSum = cards.sumOf { it.currentOutstanding }
        val netPosition = accountSum - cardOutstandingSum

        DashboardUiState(
            isLoading = false,
            netPosition = netPosition,
            accountCount = accounts.size,
            cardCount = cards.size,
            monthlyTotals = monthly,
            accounts = accounts,
            cards = cards,
            recentRange = range,
            recentTransactions = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

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
    }
}

class DashboardViewModelFactory(
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return DashboardViewModel(accountRepository, cardRepository, transactionRepository) as T
    }
}
