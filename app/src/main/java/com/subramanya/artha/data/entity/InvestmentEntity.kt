package com.subramanya.artha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.ValuationMode

/**
 * A user-tracked investment instrument (FD, SIP, MF, gold, etc.).
 *
 * `investedAmount` is COMPUTED — sum of INVESTMENT_BUY transactions whose destination
 * is this investment, minus INVESTMENT_SELL transactions whose source is this
 * investment.
 *
 * How current value is determined depends on [valuationMode] (per-type model):
 *  - DERIVED (FD/RD/PPF/EPF/Bonds): value = [openingContribution] + contributions +
 *    posted interest, computed in BalanceCalculator/InvestmentRepository.
 *  - MARKET (MF/SIP/Equity/Gold/…): value = the manually-entered [currentValue].
 * [openingContribution] seeds the contribution base for DERIVED instruments (e.g. an
 * existing balance at the time the investment was first tracked).
 *
 * [linkedInsuranceId] is set for endowment / ULIP policies — the corresponding
 * Insurance row owns the premium-cadence metadata while the Investment owns the
 * cost/return tracking.
 */
@Entity(
    tableName = "investments",
    indices = [
        Index(value = ["type"]),
        Index(value = ["linked_insurance_id"]),
        Index(value = ["is_archived"]),
    ],
)
data class InvestmentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: InvestmentType,
    /** Bank / AMC / broker — e.g. "HDFC Bank", "Zerodha", "Groww". Free text. */
    val institution: String?,
    /** Latest user-entered current value of the holding (₹). Used by MARKET mode. */
    @ColumnInfo(name = "current_value")
    val currentValue: Double,
    /** How [currentValue] is determined — DERIVED (contributions + interest) or MARKET (manual). */
    @ColumnInfo(name = "valuation_mode")
    val valuationMode: ValuationMode,
    /** Seed contribution base for DERIVED instruments (₹) — pre-existing balance when tracking began. */
    @ColumnInfo(name = "opening_contribution")
    val openingContribution: Double,
    /** Units held — applicable to MF/SIP/EQUITY/Gold-Digital. Nullable for FD/RD. */
    val units: Double?,
    /** Last-known NAV / price per unit. Nullable for FD/RD. */
    val nav: Double?,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    /** Maturity date for FD/RD/Bonds. NULL for open-ended instruments (SIP/MF/Equity). */
    @ColumnInfo(name = "maturity_date")
    val maturityDate: Long?,
    /** e.g. "80C", "80CCD(1B)" — drives the tax-section bucket view. */
    @ColumnInfo(name = "tax_section")
    val taxSection: String?,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "linked_insurance_id")
    val linkedInsuranceId: String?,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
