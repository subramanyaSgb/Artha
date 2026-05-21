package com.subramanya.artha.ui.accounts

import com.subramanya.artha.domain.model.AccountWithBalance

/** Top-level toggle: looking at active accounts or the archive bin. */
enum class AccountsView { ACTIVE, ARCHIVED }

data class AccountsUiState(
    val view: AccountsView = AccountsView.ACTIVE,
    val isReorderMode: Boolean = false,
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val archivedAccounts: List<AccountWithBalance> = emptyList(),
) {
    /** Rows currently shown given the toggle + reorder mode. */
    val shownRows: List<AccountWithBalance>
        get() = if (view == AccountsView.ACTIVE) activeAccounts else archivedAccounts
}
