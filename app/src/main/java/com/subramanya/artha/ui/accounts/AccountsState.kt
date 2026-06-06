package com.subramanya.artha.ui.accounts

import com.subramanya.artha.domain.model.AccountWithBalance

/** Top-level toggle: looking at active accounts or the archive bin. */
enum class AccountsView { ACTIVE, ARCHIVED }

/** Ordering of the account list. CUSTOM = the user's manual drag order (displayOrder). */
enum class AccountSort { CUSTOM, BALANCE_DESC, BALANCE_ASC, NAME_ASC }

data class AccountsUiState(
    val view: AccountsView = AccountsView.ACTIVE,
    val isReorderMode: Boolean = false,
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val archivedAccounts: List<AccountWithBalance> = emptyList(),
    /** True until the first data emission, so the screen can show a skeleton instead of an empty flash. */
    val isLoading: Boolean = true,
    val sort: AccountSort = AccountSort.CUSTOM,
    /** When true, rows are grouped under their account-type sub-header. */
    val groupByType: Boolean = false,
) {
    /** Rows currently shown given the toggle + reorder mode. */
    val shownRows: List<AccountWithBalance>
        get() = if (view == AccountsView.ACTIVE) activeAccounts else archivedAccounts

    /** Manual long-press reorder only makes sense on the active list in custom (unsorted, ungrouped) order. */
    val canReorder: Boolean
        get() = view == AccountsView.ACTIVE && sort == AccountSort.CUSTOM && !groupByType
}
