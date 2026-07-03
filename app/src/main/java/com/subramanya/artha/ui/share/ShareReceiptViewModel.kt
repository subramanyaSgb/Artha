package com.subramanya.artha.ui.share

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionSource
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.data.repository.AccountRepository
import com.subramanya.artha.data.repository.CategoryRepository
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.Transaction
import com.subramanya.artha.utils.ReceiptStore
import com.subramanya.artha.utils.UpiReceiptParser
import com.subramanya.artha.utils.upi.UpiParsedReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ShareReceiptUiState {
    data object Scanning : ShareReceiptUiState
    data class Parsed(
        val receipt: UpiParsedReceipt,
        val accounts: List<Account>,
        val categories: List<Category>,
        val selectedAccountId: String?,
        val selectedCategoryId: String?,
        /** Editable — pre-filled from receipt but user can correct it. */
        val description: String,
        /** Editable — pre-filled from receipt but user can correct it. */
        val amountText: String,
        val isSaving: Boolean = false,
    ) : ShareReceiptUiState
    data class Saved(val transactionId: String) : ShareReceiptUiState
    data class ScanError(val message: String) : ShareReceiptUiState
}

class ShareReceiptViewModel(
    private val imageUri: Uri,
    private val upiReceiptParser: UpiReceiptParser,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareReceiptUiState>(ShareReceiptUiState.Scanning)
    val state: StateFlow<ShareReceiptUiState> = _state.asStateFlow()

    init { scan() }

    private fun scan() {
        _state.value = ShareReceiptUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = upiReceiptParser.parse(context, imageUri)
                val accounts = accountRepository.observeActive().first()
                val categories = categoryRepository.observeAll().first()

                if (parsed == null) {
                    _state.value = ShareReceiptUiState.ScanError(
                        "Could not read a UPI receipt in this image. Try adding the transaction manually.",
                    )
                    return@launch
                }

                val matchedAccountId = parsed.sourceBankHint?.let { hint ->
                    accounts.firstOrNull { acct ->
                        acct.name.contains(hint, ignoreCase = true) ||
                            hint.contains(acct.name, ignoreCase = true)
                    }?.id
                } ?: accounts.firstOrNull()?.id

                val description = parsed.merchantName.orEmpty()
                val categoryId = autoCategory(description, categories)

                _state.value = ShareReceiptUiState.Parsed(
                    receipt = parsed,
                    accounts = accounts,
                    categories = categories,
                    selectedAccountId = matchedAccountId,
                    selectedCategoryId = categoryId,
                    description = description,
                    amountText = parsed.amount?.let { formatAmount(it) }.orEmpty(),
                )
            } catch (e: Exception) {
                _state.value = ShareReceiptUiState.ScanError(
                    e.message ?: "Failed to scan receipt.",
                )
            }
        }
    }

    fun selectAccount(accountId: String) {
        _state.update { if (it is ShareReceiptUiState.Parsed) it.copy(selectedAccountId = accountId) else it }
    }

    fun selectCategory(categoryId: String) {
        _state.update { if (it is ShareReceiptUiState.Parsed) it.copy(selectedCategoryId = categoryId) else it }
    }

    fun updateDescription(text: String) {
        _state.update { current ->
            if (current !is ShareReceiptUiState.Parsed) return@update current
            val categoryId = autoCategory(text, current.categories)
                .takeIf { it != null } ?: current.selectedCategoryId
            current.copy(description = text, selectedCategoryId = categoryId)
        }
    }

    fun updateAmount(text: String) {
        _state.update { if (it is ShareReceiptUiState.Parsed) it.copy(amountText = text) else it }
    }

    fun save() {
        val current = _state.value as? ShareReceiptUiState.Parsed ?: return
        val accountId = current.selectedAccountId ?: return
        if (current.isSaving) return

        val amount = current.amountText.replace(",", "").toDoubleOrNull() ?: 0.0

        _state.update { (it as ShareReceiptUiState.Parsed).copy(isSaving = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val receipt = current.receipt
            // Copy the shared image into app-private storage so it survives process restarts
            val savedReceiptUri = ReceiptStore.persist(context, imageUri)
            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.EXPENSE,
                amount = amount,
                currency = "INR",
                date = receipt.dateTimeMillis ?: now,
                description = current.description.ifBlank { receipt.merchantName.orEmpty() },
                categoryId = current.selectedCategoryId,
                subCategoryId = null,
                sourceType = SourceKind.ACCOUNT,
                sourceId = accountId,
                destinationType = null,
                destinationId = null,
                paymentApp = receipt.paymentApp,
                place = null,
                latitude = null,
                longitude = null,
                peopleIds = emptyList(),
                tagIds = emptyList(),
                receiptUri = savedReceiptUri,
                notes = receipt.upiRef?.let { "UPI Ref: $it" },
                taxSection = null,
                recurringRuleId = null,
                isSplit = false,
                splitGroupId = null,
                source = TransactionSource.MANUAL,
                createdAt = now,
                updatedAt = now,
            )
            transactionRepository.save(txn)
            _state.value = ShareReceiptUiState.Saved(txn.id)
        }
    }

    fun retry() = scan()

    /** Fuzzy-match [text] words against category names to guess the best category. */
    private fun autoCategory(text: String, categories: List<Category>): String? {
        if (text.isBlank()) return null
        val words = text.split(Regex("[\\s,./\\-_]+")).filter { it.length >= 3 }
        return categories.firstOrNull { cat ->
            words.any { word ->
                cat.name.contains(word, ignoreCase = true) ||
                    word.contains(cat.name, ignoreCase = true)
            }
        }?.id
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString()
        else "%.2f".format(amount)
}

class ShareReceiptViewModelFactory(
    private val imageUri: Uri,
    private val upiReceiptParser: UpiReceiptParser,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ShareReceiptViewModel(
            imageUri,
            upiReceiptParser,
            accountRepository,
            categoryRepository,
            transactionRepository,
            context,
        ) as T
}
