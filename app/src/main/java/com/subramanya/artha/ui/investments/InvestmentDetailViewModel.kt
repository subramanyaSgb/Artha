package com.subramanya.artha.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.balance.BalanceCalculator
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.mapper.toEntity
import com.subramanya.artha.data.repository.InvestmentRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class InvestmentDetailUiState(
    val investment: Investment? = null,
    /** Displayed value per the investment's valuation mode (MARKET → currentValue, DERIVED → contributions + interest). */
    val value: Double = 0.0,
    val investedAmount: Double = 0.0,
    /** Posted interest credited into this investment (only meaningful for DERIVED). */
    val interest: Double = 0.0,
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
        // Mode-aware derivation so the headline matches the list/dashboard exactly:
        //  - MARKET  → value is the manually-entered currentValue; gain = value − invested.
        //  - DERIVED → value = invested + posted interest, so gain == posted interest.
        val invested = BalanceCalculator.computeInvestmentInvested(
            investmentId,
            entities,
            investment.openingContribution,
        )
        val interest = BalanceCalculator.computeInvestmentInterest(investmentId, entities)
        val value = BalanceCalculator.computeInvestmentValue(
            investment.valuationMode,
            investment.currentValue,
            investment.openingContribution,
            investmentId,
            entities,
        )
        val gain = value - invested
        val pct = if (invested == 0.0) Double.NaN else (gain / invested) * 100.0
        InvestmentDetailUiState(
            investment = investment,
            value = value,
            investedAmount = invested,
            interest = interest,
            absoluteGain = gain,
            percentGain = pct,
            transactions = txns,
            showDeleteConfirm = deleteConfirm,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentDetailUiState())

    fun requestDelete() {
        if (state.value.investment != null) showDeleteConfirm.update { true }
    }
    fun dismissDeleteConfirm() = showDeleteConfirm.update { false }

    fun confirmDelete(onDeleted: () -> Unit) {
        val current = state.value.investment ?: return
        // Never hard-delete an investment that still has transactions — it would orphan them.
        // The UI routes to Archive instead; this is the defensive backstop.
        if (state.value.transactions.isNotEmpty()) {
            showDeleteConfirm.update { false }
            return
        }
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

    /**
     * Post an interest credit into this (DERIVED) investment. Creates an INTEREST
     * transaction with source = EXTERNAL and destination = this investment, mirroring
     * how the Invest tab builds an INVESTMENT_BUY (UUID id, MANUAL source, matching
     * created/updated timestamps). Interest counts as income in reports and grows the
     * deposit, but never touches an account balance (source is EXTERNAL, not an account).
     *
     * No-ops when [amount] <= 0 so the caller can bind it directly to a confirm button.
     * After save, the existing combine() Flow recomputes value automatically.
     */
    fun postInterest(amount: Double, dateMillis: Long) {
        if (amount <= 0.0) return
        val current = state.value.investment ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.INTEREST,
                amount = amount,
                currency = "INR",
                date = dateMillis,
                description = current.name,
                categoryId = null,
                subCategoryId = null,
                // EXTERNAL source: the interest originates outside the user's accounts,
                // so it credits the investment without debiting anything.
                sourceType = SourceKind.EXTERNAL,
                sourceId = null,
                destinationType = SourceKind.INVESTMENT,
                destinationId = current.id,
                paymentApp = "OTHER",
                place = null,
                latitude = null,
                longitude = null,
                peopleIds = emptyList(),
                tagIds = emptyList(),
                receiptUri = null,
                notes = null,
                taxSection = null,
                recurringRuleId = null,
                isSplit = false,
                splitGroupId = null,
                source = TransactionSource.MANUAL,
                createdAt = now,
                updatedAt = now,
            )
            transactionRepository.save(txn)
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
