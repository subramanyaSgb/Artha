package com.subramanya.artha.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvestmentDetailUiState(
    val investment: Investment? = null,
    val investedAmount: Double = 0.0,
    val absoluteGain: Double = 0.0,
    val percentGain: Double = Double.NaN,
    val transactions: List<Transaction> = emptyList(),
    val showDeleteConfirm: Boolean = false,
)

/**
 * Detail VM for a single Investment. Derives invested amount + gain from the
 * transaction log so the displayed numbers can never drift from the rest of the
 * app — same pattern as AccountDetailViewModel.
 */
class InvestmentDetailViewModel(
    private val investmentId: String,
    private val investmentRepository: InvestmentRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val showDeleteConfirm = MutableStateFlow(false)

    val state: StateFlow<InvestmentDetailUiState> = combine(
        investmentRepository.observeById(investmentId),
        transactionRepository.observeForAccountOrCard(investmentId),
        showDeleteConfirm,
    ) { investment, txns, deleteConfirm ->
        if (investment == null) {
            return@combine InvestmentDetailUiState(showDeleteConfirm = deleteConfirm)
        }
        val entities = txns.map { it.toEntity() }
        val invested = BalanceCalculator.computeInvestmentInvested(investmentId, entities)
        val gain = investment.currentValue - invested
        val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
        InvestmentDetailUiState(
            investment = investment,
            investedAmount = invested,
            absoluteGain = gain,
            percentGain = pct,
            transactions = txns,
            showDeleteConfirm = deleteConfirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentDetailUiState())

    fun requestDelete() {
        if (state.value.investment != null) showDeleteConfirm.update { true }
    }
    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }

    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.investment ?: return
        viewModelScope.launch {
            investmentRepository.delete(current)
            showDeleteConfirm.update { false }
            onDeleted()
        }
    }

    fun archive(onArchived: () -> Unit) {
        val current = state.value.investment ?: return
        viewModelScope.launch {
            investmentRepository.archive(current)
            onArchived()
        }
    }

    fun restore(onRestored: () -> Unit) {
        val current = state.value.investment ?: return
        viewModelScope.launch {
            investmentRepository.restore(current)
            onRestored()
        }
    }
}

class InvestmentDetailViewModelFactory(
    private val investmentId: String,
    private val investmentRepository: InvestmentRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InvestmentDetailViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return InvestmentDetailViewModel(
            investmentId,
            investmentRepository,
            transactionRepository,
        ) as T
    }
}
