package com.subramanya.artha.ui.transactions

import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Card
import com.subramanya.artha.domain.model.Category
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
)

data class TransactionsUiState(
    val query: String = "",
    val filter: TransactionsFilter = TransactionsFilter(),
    val sort: TransactionSort = TransactionSort.DATE_DESC,
    /** Day-grouped output ready for the LazyColumn. List preserves the chosen sort order. */
    val grouped: List<TransactionsGroup> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val cards: List<Card> = emptyList(),
    val categories: List<Category> = emptyList(),
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
