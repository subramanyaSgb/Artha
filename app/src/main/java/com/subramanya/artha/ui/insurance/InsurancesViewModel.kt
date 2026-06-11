package com.subramanya.artha.ui.insurance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.data.repository.InsuranceRepository
import com.subramanya.artha.data.repository.InsuranceTypeRepository
import com.subramanya.artha.domain.model.Insurance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class InsurancesViewModel(
    private val insuranceRepository: InsuranceRepository,
    private val insuranceTypeRepository: InsuranceTypeRepository,
) : ViewModel() {

    val state: StateFlow<InsurancesUiState> =
        combine(
            insuranceRepository.observeActive(),
            insuranceTypeRepository.observeAll(),
        ) { active, typeOptions ->
            val typeLabels = typeOptions.associate { it.id to it.label }
            val grouped = active.groupBy { it.type }
                .toSortedMap(compareBy { INSURANCE_TYPE_ORDER.indexOf(it) })
            val now = Clock.System.now().toEpochMilliseconds()
            val cutoff = now + DUE_SOON_WINDOW_MILLIS
            val dueSoon = active.filter {
                it.nextPremiumDate != null && it.nextPremiumDate <= cutoff
            }.sortedBy { it.nextPremiumDate }
            InsurancesUiState(
                grouped = grouped,
                typeLabels = typeLabels,
                dueWithin30Days = dueSoon,
                annualPremiumTotal = active.sumOf { it.annualisedPremium() },
                activeCount = active.size,
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsurancesUiState())

    fun archive(insurance: Insurance) {
        viewModelScope.launch { insuranceRepository.archive(insurance) }
    }

    fun delete(insurance: Insurance) {
        viewModelScope.launch { insuranceRepository.delete(insurance) }
    }

    private companion object {
        /** "Due soon" = next 30 days, per PRD §7.12. */
        const val DUE_SOON_WINDOW_MILLIS: Long = 30L * 24 * 60 * 60 * 1000
    }
}

/** Premium normalised to ₹/year so the hero total stays comparable across cadences. */
internal fun Insurance.annualisedPremium(): Double = when (premiumFrequency) {
    PremiumFrequency.MONTHLY -> premiumAmount * 12.0
    PremiumFrequency.QUARTERLY -> premiumAmount * 4.0
    PremiumFrequency.HALF_YEARLY -> premiumAmount * 2.0
    PremiumFrequency.YEARLY -> premiumAmount
    // SINGLE-pay policies are amortised to zero/year — they're a one-shot cost; user
    // already paid up front. Hero shows recurring outgo only.
    PremiumFrequency.SINGLE -> 0.0
}

class InsurancesViewModelFactory(
    private val insuranceRepository: InsuranceRepository,
    private val insuranceTypeRepository: InsuranceTypeRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InsurancesViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return InsurancesViewModel(insuranceRepository, insuranceTypeRepository) as T
    }
}
