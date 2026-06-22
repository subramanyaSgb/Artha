package com.subramanya.artha.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.ui.common.ArthaSheetHandle
import com.subramanya.artha.ui.common.FieldRow
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.common.SheetTitle
import com.subramanya.artha.ui.common.SheetWindowInsets
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.utils.IndianNumberFormat
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet for withdrawing from an investment (FD, RD, etc.).
 *
 * Allows the user to specify:
 * - Withdrawal amount (validates > 0 and <= investment.currentValue)
 * - Destination account (filtered to active accounts only)
 *
 * On confirmation, calls onConfirm with the amount and destination account ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalSheet(
    investment: Investment,
    accounts: List<Account>,
    onConfirm: (amount: Double, destinationAccountId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var withdrawalAmountText by remember { mutableStateOf("") }
    var selectedDestinationId by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Filter to active accounts only
    val activeAccounts = accounts.filter { !it.isArchived }

    // Parse the withdrawal amount
    val withdrawalAmount = remember(withdrawalAmountText) {
        if (withdrawalAmountText.isBlank()) null else withdrawalAmountText.toDoubleOrNull()
    }

    // Validate: amount must be > 0 and <= currentValue
    val isValidAmount = withdrawalAmount != null && withdrawalAmount > 0.0 && withdrawalAmount <= investment.currentValue
    val isValidAccount = selectedDestinationId.isNotEmpty()
    val isFormValid = isValidAmount && isValidAccount

    // Compute remaining balance (updates live as user changes amount)
    val remainingBalance = if (isValidAmount) {
        investment.currentValue - withdrawalAmount
    } else {
        investment.currentValue
    }

    // Get the selected account for display
    val selectedAccount = activeAccounts.find { it.id == selectedDestinationId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentWindowInsets = SheetWindowInsets,
        dragHandle = { ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            SheetTitle(title = stringResource(R.string.withdrawal_sheet_title, investment.name))

            // Current balance display (read-only, teal background)
            FieldRow(label = stringResource(R.string.withdrawal_sheet_current_balance)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Teal500.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = IndianNumberFormat.format(investment.currentValue),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                        ),
                        color = Teal500,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Amount input field
            FieldRow(label = stringResource(R.string.withdrawal_sheet_amount_label)) {
                OutlinedTextField(
                    value = withdrawalAmountText,
                    onValueChange = { value ->
                        // Allow digits and a single decimal point only
                        withdrawalAmountText = value.filterIndexed { index, c ->
                            c.isDigit() || (c == '.' && value.indexOf('.') == index)
                        }
                    },
                    singleLine = true,
                    placeholder = { Text("25000") },
                    suffix = { Text("₹") },
                    isError = showErrors && !isValidAmount && withdrawalAmountText.isNotBlank(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showErrors && withdrawalAmountText.isBlank()) {
                    Text(
                        text = stringResource(R.string.withdrawal_sheet_amount_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else if (showErrors && withdrawalAmount != null && withdrawalAmount <= 0.0) {
                    Text(
                        text = stringResource(R.string.withdrawal_sheet_amount_positive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else if (showErrors && withdrawalAmount != null && withdrawalAmount > investment.currentValue) {
                    Text(
                        text = stringResource(R.string.withdrawal_sheet_amount_exceeds, IndianNumberFormat.format(investment.currentValue)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Destination account dropdown
            FieldRow(label = stringResource(R.string.withdrawal_sheet_destination_account)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedAccount?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.withdrawal_sheet_select_account)) },
                        suffix = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        isError = showErrors && !isValidAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true },
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f),
                    ) {
                        activeAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedDestinationId = account.id
                                    expandedDropdown = false
                                },
                            )
                        }
                    }
                }
                if (showErrors && !isValidAccount && selectedDestinationId.isBlank()) {
                    Text(
                        text = stringResource(R.string.withdrawal_sheet_account_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Confirmation text: "₹X will credit to Y Account"
            if (isValidAmount && selectedAccount != null) {
                FieldRow(label = stringResource(R.string.withdrawal_sheet_summary)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.withdrawal_sheet_confirmation,
                                IndianNumberFormat.format(withdrawalAmount),
                                selectedAccount.name,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            // Remaining balance preview
            FieldRow(label = stringResource(R.string.withdrawal_sheet_remaining_balance)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.withdrawal_sheet_after_withdrawal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = IndianNumberFormat.format(remainingBalance),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (remainingBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Cancel and Confirm buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        if (!isFormValid) {
                            showErrors = true
                            return@Button
                        }
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                        onConfirm(withdrawalAmount!!, selectedDestinationId)
                    },
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.withdrawal_sheet_confirm))
                }
            }
        }
    }
}
