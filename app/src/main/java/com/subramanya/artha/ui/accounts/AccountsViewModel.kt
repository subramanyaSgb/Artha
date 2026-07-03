package com.subramanya.artha.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.R
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.domain.model.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val view = MutableStateFlow(AccountsView.ACTIVE)
    private val reorderMode = MutableStateFlow(false)
    private val sort = MutableStateFlow(AccountSort.CUSTOM)
    private val groupByType = MutableStateFlow(false)

    /** One-shot string-res message for a toast (e.g. delete-blocked → archived instead). */
    private val message = MutableStateFlow<Int?>(null)
    val toastMessage: StateFlow<Int?> = message.asStateFlow()
    fun consumeToast() = message.update { null }

    /**
     * Live state for the screen. Archived rows surface their *opening* balance only —
     * treating "archived = frozen" matches the typical mental model and avoids fanning
     * out a per-account live-balance flow for rows the user has explicitly retired.
     */
    private data class UiBag(
        val view: AccountsView,
        val reorder: Boolean,
        val sort: AccountSort,
        val group: Boolean,
    )

    val state: StateFlow<AccountsUiState> = combine(
        accountRepository.observeActiveWithBalances(),
        accountRepository.observeArchived(),
        combine(view, reorderMode, sort, groupByType) { v, r, s, g -> UiBag(v, r, s, g) },
    ) { active, archived, ui ->
        val archivedWithBalance = archived.map { account ->
            com.subramanya.artha.domain.model.AccountWithBalance(account, account.openingBalance)
        }
        // Reorder only applies on the active list in CUSTOM, ungrouped order — any sort/group
        // overrides displayOrder, so dragging there would be meaningless.
        val canReorder = ui.view == AccountsView.ACTIVE && ui.sort == AccountSort.CUSTOM && !ui.group
        AccountsUiState(
            view = ui.view,
            isReorderMode = ui.reorder && canReorder,
            activeAccounts = order(active, ui.sort, ui.group),
            archivedAccounts = order(archivedWithBalance, ui.sort, ui.group),
            isLoading = false,
            sort = ui.sort,
            groupByType = ui.group,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    /** Apply the chosen sort, then (if grouping) a stable secondary sort by type so rows of the
     *  same type sit together while preserving the primary order within each type. CUSTOM keeps
     *  the DAO's displayOrder. */
    private fun order(
        list: List<com.subramanya.artha.domain.model.AccountWithBalance>,
        sort: AccountSort,
        group: Boolean,
    ): List<com.subramanya.artha.domain.model.AccountWithBalance> {
        val sorted = when (sort) {
            AccountSort.CUSTOM -> list
            AccountSort.BALANCE_DESC -> list.sortedByDescending { it.currentBalance }
            AccountSort.BALANCE_ASC -> list.sortedBy { it.currentBalance }
            AccountSort.NAME_ASC -> list.sortedBy { it.account.name.lowercase() }
        }
        return if (group) sorted.sortedBy { it.account.type.lowercase() } else sorted
    }

    fun setSort(value: AccountSort) {
        sort.update { value }
        if (value != AccountSort.CUSTOM) reorderMode.update { false }
    }

    fun toggleGroupByType() {
        groupByType.update { !it }
        reorderMode.update { false }
    }

    // ---------- view toggles ----------

    fun showActive() {
        view.update { AccountsView.ACTIVE }
    }

    fun showArchived() {
        view.update { AccountsView.ARCHIVED }
        // Reorder mode only makes sense on the active list.
        reorderMode.update { false }
    }

    fun enterReorderMode() {
        // Only meaningful in the active list with custom (manual) order and no grouping.
        if (view.value == AccountsView.ACTIVE && sort.value == AccountSort.CUSTOM && !groupByType.value) {
            reorderMode.update { true }
        }
    }

    fun exitReorderMode() {
        reorderMode.update { false }
    }

    // ---------- archive / restore ----------

    fun archive(account: Account) {
        viewModelScope.launch { accountRepository.archive(account) }
    }

    fun restore(account: Account) {
        viewModelScope.launch { accountRepository.restore(account) }
    }

    /**
     * Hard delete — but ONLY when no transaction references this account (there is no FK to
     * cascade through; sourceId/destinationId are polymorphic). If transactions reference it,
     * deleting would orphan them and distort reports, so we archive instead and toast. This
     * mirrors the guard on the detail screen so the list path can't bypass it.
     */
    fun delete(account: Account) {
        viewModelScope.launch {
            if (accountRepository.hasReferencingTransactions(account.id)) {
                accountRepository.archive(account)
                message.update { R.string.entity_delete_archived_instead }
            } else {
                accountRepository.delete(account)
            }
        }
    }

    // ---------- reorder ----------

    /**
     * Swap the displayOrder of [account] with its neighbour in the given direction.
     * Persists both rows immediately so the order survives an app restart.
     */
    fun moveUp(account: Account) = moveBy(account, -1)
    fun moveDown(account: Account) = moveBy(account, +1)

    private fun moveBy(account: Account, delta: Int) {
        val snapshot = state.value.activeAccounts.map { it.account }
        val index = snapshot.indexOfFirst { it.id == account.id }
        if (index < 0) return
        val targetIndex = index + delta
        if (targetIndex !in snapshot.indices) return
        val a = snapshot[index]
        val b = snapshot[targetIndex]
        viewModelScope.launch {
            accountRepository.update(a.copy(displayOrder = b.displayOrder))
            accountRepository.update(b.copy(displayOrder = a.displayOrder))
        }
    }
}

class AccountsViewModelFactory(
    private val accountRepository: AccountRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AccountsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AccountsViewModel(accountRepository) as T
    }
}
