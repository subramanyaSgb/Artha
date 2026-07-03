package com.subramanya.artha.ui.share

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.InlineTopBar
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.ExpenseSoft
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.UpiReceiptParser
import com.subramanya.artha.utils.upi.UpiParsedReceipt

@Composable
fun ShareReceiptScreen(
    imageUriString: String,
    onBack: () -> Unit,
    onTransactionSaved: (transactionId: String) -> Unit,
    onAddManually: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: ShareReceiptViewModel = viewModel(
        factory = ShareReceiptViewModelFactory(
            imageUri = Uri.parse(imageUriString),
            upiReceiptParser = UpiReceiptParser(),
            accountRepository = app.accountRepository,
            transactionRepository = app.transactionRepository,
            context = context,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    // Navigate away when saved
    LaunchedEffect(state) {
        if (state is ShareReceiptUiState.Saved) {
            onTransactionSaved((state as ShareReceiptUiState.Saved).transactionId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        InlineTopBar(
            title = stringResource(R.string.share_receipt_title),
            onBack = onBack,
        )

        AnimatedContent(
            targetState = state,
            label = "share-receipt-state",
        ) { currentState ->
            when (currentState) {
                is ShareReceiptUiState.Scanning -> ScanningContent()
                is ShareReceiptUiState.Parsed -> ParsedContent(
                    state = currentState,
                    onSelectAccount = vm::selectAccount,
                    onSave = vm::save,
                    onAddManually = onAddManually,
                )
                is ShareReceiptUiState.Saved -> ScanningContent() // brief flash before nav
                is ShareReceiptUiState.ScanError -> ErrorContent(
                    message = currentState.message,
                    onRetry = vm::retry,
                    onAddManually = onAddManually,
                )
            }
        }
    }
}

@Composable
private fun ScanningContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Teal500, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.share_receipt_scanning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onAddManually: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.share_receipt_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
        ) {
            Text(stringResource(R.string.share_receipt_retry))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAddManually,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.share_receipt_add_manually))
        }
    }
}

@Composable
private fun ParsedContent(
    state: ShareReceiptUiState.Parsed,
    onSelectAccount: (String) -> Unit,
    onSave: () -> Unit,
    onAddManually: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero amount card
        AmountHeroCard(receipt = state.receipt)

        Spacer(Modifier.height(16.dp))

        // Parsed detail fields
        ReceiptFieldsCard(receipt = state.receipt)

        Spacer(Modifier.height(16.dp))

        // Account picker
        AccountPickerSection(
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onSelect = onSelectAccount,
        )

        Spacer(Modifier.height(24.dp))

        // Actions
        Button(
            onClick = onSave,
            enabled = state.selectedAccountId != null && !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp,
                )
            } else {
                val amount = state.receipt.amount
                val label = if (amount != null) {
                    stringResource(R.string.share_receipt_save_with_amount, IndianNumberFormat.format(amount))
                } else {
                    stringResource(R.string.share_receipt_save)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAddManually,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(stringResource(R.string.share_receipt_add_manually))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AmountHeroCard(receipt: UpiParsedReceipt) {
    Surface(
        color = ExpenseSoft,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Expense.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = Teal500,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.share_receipt_phonepe_label).uppercase(),
                    style = EyebrowStyle,
                    color = Teal500,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (receipt.amount != null) {
                Text(
                    text = "₹${IndianNumberFormat.format(receipt.amount)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                    ),
                    color = Expense,
                )
            } else {
                Text(
                    text = stringResource(R.string.share_receipt_amount_unknown),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (receipt.merchantName != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = receipt.merchantName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ReceiptFieldsCard(receipt: UpiParsedReceipt) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.share_receipt_details_label).uppercase(),
                style = EyebrowStyle,
                color = Text3,
            )
            Spacer(Modifier.height(12.dp))

            receipt.dateTimeMillis?.let { millis ->
                ReceiptField(
                    label = stringResource(R.string.share_receipt_field_date),
                    value = DateFormatter.longDate(millis),
                )
                Spacer(Modifier.height(10.dp))
            }

            receipt.upiRef?.let { ref ->
                ReceiptField(
                    label = stringResource(R.string.share_receipt_field_upi_ref),
                    value = ref,
                    mono = true,
                )
                Spacer(Modifier.height(10.dp))
            }

            receipt.sourceBankHint?.let { bank ->
                ReceiptField(
                    label = stringResource(R.string.share_receipt_field_paid_from),
                    value = bank,
                )
            }
        }
    }
}

@Composable
private fun ReceiptField(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Text2,
        )
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            } else {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AccountPickerSection(
    accounts: List<com.subramanya.artha.domain.model.Account>,
    selectedAccountId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedAccountId }

    Column {
        Text(
            text = stringResource(R.string.share_receipt_account_label).uppercase(),
            style = EyebrowStyle,
            color = Text3,
        )
        Spacer(Modifier.height(8.dp))

        Box {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (selected != null) LineTeal else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = accounts.isNotEmpty()) { expanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = if (selected != null) Teal500 else Text3,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = selected?.name ?: stringResource(R.string.share_receipt_account_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected != null) MaterialTheme.colorScheme.onSurface else Text3,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        trailingIcon = if (account.id == selectedAccountId) ({
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Income,
                                modifier = Modifier.size(16.dp),
                            )
                        }) else null,
                        onClick = {
                            onSelect(account.id)
                            expanded = false
                        },
                    )
                }
            }
        }

        if (selected == null && accounts.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.share_receipt_account_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
