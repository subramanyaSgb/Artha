package com.subramanya.artha.ui.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.db.seed.SeedPaymentApps
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.PendingSmsRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.PendingSms
import com.subramanya.artha.domain.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingSmsUiState(
    val items: List<PendingSms> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Drives the SMS review queue: lists parsed-but-unconfirmed SMS and turns a confirmed one
 * into a real [Transaction] (source = SMS). Dismissing just drops the pending row.
 */
class PendingSmsViewModel(
    private val pendingSmsRepository: PendingSmsRepository,
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<PendingSmsUiState> = combine(
        pendingSmsRepository.observeAll(),
        accountRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { items, accounts, categories ->
        PendingSmsUiState(items = items, accounts = accounts, categories = categories, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingSmsUiState())

    /**
     * Confirms a pending SMS into a transaction. [accountId] is required (the money-affected
     * account). Debit → EXPENSE, credit → INCOME. The pending row is removed on success.
     */
    fun confirm(
        pending: PendingSms,
        accountId: String,
        categoryId: String?,
        amount: Double,
        description: String,
        onSaved: (transactionId: String) -> Unit,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                type = if (pending.isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
                amount = amount,
                currency = "INR",
                date = pending.effectiveDate,
                description = description.ifBlank { pending.merchant.orEmpty() },
                categoryId = categoryId,
                subCategoryId = null,
                sourceType = SourceKind.ACCOUNT,
                sourceId = accountId,
                destinationType = null,
                destinationId = null,
                paymentApp = SeedPaymentApps.DEFAULT_ID,
                place = null,
                latitude = null,
                longitude = null,
                peopleIds = emptyList(),
                tagIds = emptyList(),
                receiptUri = null,
                notes = pending.refNo?.let { "UPI Ref: $it" },
                taxSection = null,
                recurringRuleId = null,
                isSplit = false,
                splitGroupId = null,
                source = TransactionSource.SMS,
                createdAt = now,
                updatedAt = now,
            )
            transactionRepository.save(txn)
            pendingSmsRepository.dismiss(pending.id)
            onSaved(txn.id)
        }
    }

    fun dismiss(id: String) {
        viewModelScope.launch { pendingSmsRepository.dismiss(id) }
    }

    fun dismissAll() {
        viewModelScope.launch { pendingSmsRepository.clear() }
    }
}

class PendingSmsViewModelFactory(
    private val pendingSmsRepository: PendingSmsRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PendingSmsViewModel(
            pendingSmsRepository,
            transactionRepository,
            accountRepository,
            categoryRepository,
        ) as T
}
