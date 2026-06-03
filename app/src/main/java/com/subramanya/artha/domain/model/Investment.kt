package com.subramanya.artha.domain.model

import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.ValuationMode

data class Investment(
    val id: String,
    val name: String,
    val type: InvestmentType,
    val institution: String?,
    val currentValue: Double,
    val valuationMode: ValuationMode,
    val openingContribution: Double,
    val units: Double?,
    val nav: Double?,
    val startDate: Long,
    val maturityDate: Long?,
    val taxSection: String?,
    val icon: String,
    val color: Long,
    val linkedInsuranceId: String?,
    val isArchived: Boolean,
    val displayOrder: Int,
    val createdAt: Long,
)

/** Domain view that pairs the stored entity with its derived invested-amount + return %. */
data class InvestmentWithMetrics(
    val investment: Investment,
    /** Sum of BUY transactions minus SELL transactions — what the user actually put in. */
    val investedAmount: Double,
    /** Displayed value per the investment's valuation mode (MARKET → currentValue, DERIVED → contributions + interest). */
    val value: Double,
    /** value − invested */
    val absoluteGain: Double,
    /** Percent gain. NaN when investedAmount is 0 — UI shows "—" in that case. */
    val percentGain: Double,
)
