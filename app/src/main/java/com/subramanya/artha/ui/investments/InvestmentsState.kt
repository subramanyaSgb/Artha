package com.subramanya.artha.ui.investments

import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.domain.model.InvestmentWithMetrics

/** Tab choice — "By Type" groups rows under their InvestmentType header, "All" is flat. */
enum class InvestmentsView { ALL, BY_TYPE }

data class InvestmentsUiState(
    val view: InvestmentsView = InvestmentsView.ALL,
    val rows: List<InvestmentWithMetrics> = emptyList(),
    /** Pre-grouped for the BY_TYPE view; same data as [rows] keyed by type. */
    val grouped: Map<InvestmentType, List<InvestmentWithMetrics>> = emptyMap(),
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
)
