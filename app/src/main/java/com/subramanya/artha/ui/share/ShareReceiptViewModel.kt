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
import com.subramanya.artha.data.repository.TransactionRepository
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Transaction
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
        val selectedAccountId: String?,
        val isSaving: Boolean = false,
    ) : ShareReceiptUiState
    data class Saved(val transactionId: String) : ShareReceiptUiState
    data class ScanError(val message: String) : ShareReceiptUiState
}

class ShareReceiptViewModel(
    private val imageUri: Uri,
    private val upiReceiptParser: UpiReceiptParser,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareReceiptUiState>(ShareReceiptUiState.Scanning)
    val state: StateFlow<ShareReceiptUiState> = _state.asStateFlow()

    init {
        scan()
    }

    private fun scan() {
        _state.value = ShareReceiptUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = upiReceiptParser.parse(context, imageUri)
                val accounts = accountRepository.observeActive().first()

                if (parsed == null) {
                    _state.value = ShareReceiptUiState.ScanError(
                        "Could not read a UPI receipt in this image. Try adding the transaction manually.",
                    )
                    return@launch
                }

                // Try to match the bank hint to an existing account by name
                val matchedId = parsed.sourceBankHint?.let { hint ->
                    accounts.firstOrNull { acct ->
                        acct.name.contains(hint, ignoreCase = true) ||
                            hint.contains(acct.name, ignoreCase = true)
                    }?.id
                } ?: accounts.firstOrNull()?.id

                _state.value = ShareReceiptUiState.Parsed(
                    receipt = parsed,
                    accounts = accounts,
                    selectedAccountId = matchedId,
                )
            } catch (e: Exception) {
                _state.value = ShareReceiptUiState.ScanError(
                    e.message ?: "Failed to scan receipt.",
                )
            }
        }
    }

    fun selectAccount(accountId: String) {
        _state.update { current ->
            if (current is ShareReceiptUiState.Parsed) current.copy(selectedAccountId = accountId)
            else current
        }
    }

    fun save() {
        val current = _state.value as? ShareReceiptUiState.Parsed ?: return
        val accountId = current.selectedAccountId ?: return
        if (current.isSaving) return

        _state.update { (it as ShareReceiptUiState.Parsed).copy(isSaving = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val receipt = current.receipt
            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.EXPENSE,
                amount = receipt.amount ?: 0.0,
                currency = "INR",
                date = receipt.dateTimeMillis ?: now,
                description = receipt.merchantName.orEmpty(),
                categoryId = null,
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
                receiptUri = null,
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
}

class ShareReceiptViewModelFactory(
    private val imageUri: Uri,
    private val upiReceiptParser: UpiReceiptParser,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ShareReceiptViewModel(
            imageUri,
            upiReceiptParser,
            accountRepository,
            transactionRepository,
            context,
        ) as T
}
