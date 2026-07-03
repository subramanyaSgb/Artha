package com.subramanya.artha.ui.dashboard

import com.subramanya.artha.data.balance.MonthlyTotals
import com.subramanya.artha.domain.model.AccountWithBalance
import com.subramanya.artha.domain.model.CardWithBalance
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.Transaction

data class DashboardUiState(
    val isLoading: Boolean = true,
    val netPosition: Double = 0.0,
    val accountCount: Int = 0,
    val cardCount: Int = 0,
    val monthlyTotals: MonthlyTotals = MonthlyTotals.ZERO,
    val accounts: List<AccountWithBalance> = emptyList(),
    val cards: List<CardWithBalance> = emptyList(),
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
    /** True change in net position this calendar month: net position now minus net
     *  position at the instant before the month began (the current investment value
     *  cancels on both sides, so this is the realized liquid + card movement). Drives
     *  the "↑ ₹12,400 · +2.6%" hero row. */
    val netChangeThisMonth: Double = 0.0,
    /** id -> Category for resolving each recent-activity row's real icon, colour and
     *  name. Without this the rows fall back to a type icon and an id-derived label
     *  (which renders a raw UUID for user-created categories). */
    val categoriesById: Map<String, Category> = emptyMap(),
    /** Top expense categories this calendar month, highest first, for the
     *  "where it went" breakdown. Empty when there are no expenses yet. */
    val topSpending: List<CategorySpend> = emptyList(),
)

/** One row of the spending breakdown: a category ([category] null = uncategorised)
 *  and its total EXPENSE amount this calendar month. */
data class CategorySpend(
    val category: Category?,
    val amount: Double,
)

/**
 * The dashboard's reorderable content sections (the Net Position hero, premium-due banner
 * and the opt-in AI card stay pinned, so they're not listed here). [key] is the stable id
 * persisted in DataStore; [DEFAULT_ORDER] is the canonical top-to-bottom order.
 */
enum class DashboardSection(val key: String) {
    MONTHLY("monthly"),
    SPENDING("spending"),
    ACCOUNTS("accounts"),
    CARDS("cards"),
    RECENT("recent"),
    ;

    companion object {
        /**
         * Resolve a saved key list to sections: drop unknown keys, then append any canonical
         * sections the saved list is missing — so a newly-added section can never be lost
         * behind a stale saved order, and a removed/renamed key is ignored gracefully.
         */
        fun ordered(savedKeys: List<String>): List<DashboardSection> {
            val byKey = entries.associateBy { it.key }
            val resolved = savedKeys.mapNotNull { byKey[it] }
            return resolved + entries.filter { it !in resolved }
        }
    }
}
