package com.subramanya.artha.ui.insurance

import com.subramanya.artha.domain.model.Insurance

/** Stable display order for the grouped list (built-in ids) — Health first, OTHER last. */
internal val INSURANCE_TYPE_ORDER: List<String> = listOf(
    "HEALTH", "VEHICLE", "LIFE_TERM", "LIFE_ENDOWMENT", "TRAVEL", "HOME", "OTHER",
)

data class InsurancesUiState(
    val grouped: Map<String, List<Insurance>> = emptyMap(),
    val typeLabels: Map<String, String> = emptyMap(),
    val dueWithin30Days: List<Insurance> = emptyList(),
    val annualPremiumTotal: Double = 0.0,
    val activeCount: Int = 0,
)
