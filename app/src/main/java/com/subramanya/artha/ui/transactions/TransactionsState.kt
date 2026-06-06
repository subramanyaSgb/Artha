package com.subramanya.artha.ui.transactions

import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Tag
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.utils.TimeRange

enum class TransactionSort { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }

/** Composable filter spec — all-null means "no filter on that dimension". */
data class TransactionsFilter(
    val range: TimeRange = TimeRange.ALL_TIME,
    val typeFilter: TransactionType? = null,
    val accountId: String? = null,
    val cardId: String? = null,
    val categoryId: String? = null,
    val tagId: String? = null,
)

data class TransactionsUiState(
    val query: String = "",
    val filter: TransactionsFilter = TransactionsFilter(),
    val sort: TransactionSort = TransactionSort.DATE_DESC,
    /**
     * Flattened day-grouped rows ready for a virtualized LazyColumn: each transaction is its own
     * keyed item (so scroll/selection recomposes one row, not the whole day block). Day-card
     * corner rounding is driven by the per-entry first/last flags. Preserves the chosen sort.
     */
    val rows: List<LedgerListItem> = emptyList(),
    /** categoryId → Category, precomputed once so rows resolve their icon without re-scanning. */
    val categoriesById: Map<String, Category> = emptyMap(),
    /** In/Out/Net totals for the visible (filtered) set, precomputed off the UI thread. */
    val inSum: Double = 0.0,
    val outSum: Double = 0.0,
    val net: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val cards: List<Card> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    /** Number of transactions in the visible (filtered) set, for the result-count label. */
    val count: Int = 0,
    val selectedIds: Set<String> = emptySet(),
    val showDeleteConfirm: Boolean = false,
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
}

/** Section header + the transactions falling under that header for the chosen group-by. */
data class TransactionsGroup(
    /** Header key used as Compose `key` and for stable identity. */
    val headerKey: String,
    /** Display string for the section header ("Today", "Yesterday", "Mon, 19 May", …). */
    val headerDisplay: String,
    val transactions: List<Transaction>,
)

/** One flattened entry in the Ledger list — a day header or a single transaction row. */
sealed interface LedgerListItem {
    /** Stable Compose key. */
    val key: String

    data class DayHeader(
        val headerKey: String,
        val display: String,
        /** Signed sum of the day's transactions for the header's right-aligned total. */
        val daySum: Double,
    ) : LedgerListItem {
        override val key: String get() = "h-$headerKey"
    }

    data class Entry(
        val txn: Transaction,
        val category: Category?,
        /** Drives the day-card corner rounding + the inter-row divider. */
        val isFirstInDay: Boolean,
        val isLastInDay: Boolean,
    ) : LedgerListItem {
        override val key: String get() = "t-${txn.id}"
    }
}
