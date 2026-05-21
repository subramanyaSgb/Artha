package com.subramanya.artha.ui.insurance

import com.subramanya.artha.data.entity.enums.InsuranceType
import com.subramanya.artha.domain.model.Insurance

/** Stable display order for the grouped list — Health first, OTHER last. */
internal val INSURANCE_TYPE_ORDER: List<InsuranceType> = listOf(
    InsuranceType.HEALTH,
    InsuranceType.VEHICLE,
    InsuranceType.LIFE_TERM,
    InsuranceType.LIFE_ENDOWMENT,
    InsuranceType.TRAVEL,
    InsuranceType.HOME,
    InsuranceType.OTHER,
)

data class InsurancesUiState(
    val grouped: Map<InsuranceType, List<Insurance>> = emptyMap(),
    val dueWithin30Days: List<Insurance> = emptyList(),
    /** Annualised premium total across all active policies — drives the hero card. */
    val annualPremiumTotal: Double = 0.0,
    val activeCount: Int = 0,
)
