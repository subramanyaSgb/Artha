package com.subramanya.artha.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.domain.model.Investment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvestmentsViewModel(
    private val investmentRepository: InvestmentRepository,
) : ViewModel() {

    private val view = MutableStateFlow(InvestmentsView.ALL)

    val state: StateFlow<InvestmentsUiState> = combine(
        investmentRepository.observeActiveWithMetrics(),
        view,
    ) { metrics, currentView ->
        InvestmentsUiState(
            view = currentView,
            rows = metrics,
            grouped = metrics.groupBy { it.investment.type },
            totalInvested = metrics.sumOf { it.investedAmount },
            totalCurrentValue = metrics.sumOf { it.investment.currentValue },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsUiState())

    fun showAll() = view.update { InvestmentsView.ALL }
    fun showByType() = view.update { InvestmentsView.BY_TYPE }

    fun archive(investment: Investment) {
        viewModelScope.launch { investmentRepository.archive(investment) }
    }

    fun delete(investment: Investment) {
        viewModelScope.launch { investmentRepository.delete(investment) }
    }
}

class InvestmentsViewModelFactory(
    private val investmentRepository: InvestmentRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InvestmentsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return InvestmentsViewModel(investmentRepository) as T
    }
}
