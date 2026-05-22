package com.subramanya.artha.ui.dashboard

import com.subramanya.artha.data.balance.MonthlyTotals
import com.subramanya.artha.domain.model.AccountWithBalance
import com.subramanya.artha.domain.model.CardWithBalance
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.utils.TimeRange

data class DashboardUiState(
    val isLoading: Boolean = true,
    val netPosition: Double = 0.0,
    val accountCount: Int = 0,
    val cardCount: Int = 0,
    val monthlyTotals: MonthlyTotals = MonthlyTotals.ZERO,
    val accounts: List<AccountWithBalance> = emptyList(),
    val cards: List<CardWithBalance> = emptyList(),
    val recentRange: TimeRange = TimeRange.TODAY,
    val recentTransactions: List<Transaction> = emptyList(),
    /** Insurance policies with next-due within the upcoming week. Drives the
     *  "Premium due soon" banner on Dashboard so the user notices without
     *  having to drill into the Insurance screen. */
    val premiumsDueThisWeek: List<Insurance> = emptyList(),
    /** Net investment value across all active investments — joins Net Position
     *  hero so the displayed "Net" includes paper wealth, not just liquid cash. */
    val investmentTotalValue: Double = 0.0,
    /** End-of-day net position for the last 30 days, oldest -> newest. Drives
     *  the hero sparkline. Empty when there's nothing to chart yet. */
    val netPositionSpark: List<Double> = emptyList(),
    /** Change in net position this calendar month (income - expense - card-payment net
     *  applied to liquid + invest movements). Used for the "↑ ₹12,400 · +2.6%" row. */
    val netChangeThisMonth: Double = 0.0,
)
