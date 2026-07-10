package com.subramanya.artha.ui.share

import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.ui.common.ArthaDatePickerDialog
import com.subramanya.artha.ui.common.ArthaTimePickerDialog
import com.subramanya.artha.data.entity.enums.CategoryType
import com.subramanya.artha.ui.common.InlineTopBar
import com.subramanya.artha.ui.common.mergeTimeKeepingDate
import com.subramanya.artha.ui.transaction.CategoryPickerSheet
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.ExpenseSoft
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.UpiReceiptParser
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            upiReceiptParser = UpiReceiptParser(
                nimKeyProvider = { app.nimApiKey() },
                openRouterKeyProvider = { app.openRouterApiKey() },
            ),
            accountRepository = app.accountRepository,
            cardRepository = app.cardRepository,
            categoryRepository = app.categoryRepository,
            paymentAppRepository = app.paymentAppRepository,
            transactionRepository = app.transactionRepository,
            context = context,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        (state as? ShareReceiptUiState.Saved)?.let { onTransactionSaved(it.transactionId) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        InlineTopBar(title = stringResource(R.string.share_receipt_title), onBack = onBack)
        when (val s = state) {
            is ShareReceiptUiState.Scanning, is ShareReceiptUiState.Saved -> ScanningContent()
            is ShareReceiptUiState.ScanError -> ErrorContent(s.message, vm::retry, onAddManually)
            is ShareReceiptUiState.Parsed -> ParsedContent(
                state = s,
                onAmountChange = vm::updateAmount,
                onMerchantChange = vm::updateMerchant,
                onDescriptionChange = vm::updateDescription,
                onDateTimeChange = vm::updateDateTime,
                onSelectAccount = vm::selectAccount,
                onSelectCategory = vm::selectCategory,
                onSelectPaymentApp = vm::selectPaymentApp,
                onSave = vm::save,
                onAddManually = onAddManually,
            )
        }
    }
}

@Composable
private fun ScanningContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
private fun ErrorContent(message: String, onRetry: () -> Unit, onAddManually: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.share_receipt_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = Text3, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
        ) { Text(stringResource(R.string.share_receipt_retry)) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAddManually,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text(stringResource(R.string.share_receipt_add_manually)) }
    }
}

@Composable
private fun ParsedContent(
    state: ShareReceiptUiState.Parsed,
    onAmountChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateTimeChange: (Long) -> Unit,
    onSelectAccount: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectPaymentApp: (String) -> Unit,
    onSave: () -> Unit,
    onAddManually: () -> Unit,
) {
    // Text fields hold local state (init once) so fast typing never jumbles against the VM flow.
    var amount by remember { mutableStateOf(state.amountText) }
    var merchant by remember { mutableStateOf(state.merchant) }
    var description by remember { mutableStateOf(state.description) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val parsedAmount = amount.replace(",", "").toDoubleOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(12.dp))

        // Amount hero
        AmountField(value = amount, onValueChange = { amount = it; onAmountChange(it) })
        Spacer(Modifier.height(16.dp))

        // Date + Time
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                label = stringResource(R.string.share_receipt_field_date),
                value = DateFormatter.longDate(state.dateTimeMillis),
                onClick = { showDatePicker = true },
            )
            FieldTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Schedule,
                label = stringResource(R.string.share_receipt_field_time),
                value = formatTime(state.dateTimeMillis),
                onClick = { showTimePicker = true },
            )
        }
        Spacer(Modifier.height(12.dp))

        // Merchant
        LabeledTextField(
            label = stringResource(R.string.share_receipt_field_merchant),
            value = merchant,
            onValueChange = { merchant = it; onMerchantChange(it) },
        )
        Spacer(Modifier.height(12.dp))

        // Description
        LabeledTextField(
            label = stringResource(R.string.share_receipt_field_description),
            value = description,
            onValueChange = { description = it; onDescriptionChange(it) },
        )
        Spacer(Modifier.height(12.dp))

        // Payment app
        DropdownField(
            icon = Icons.Filled.Payments,
            label = stringResource(R.string.share_receipt_field_payment_app),
            selectedLabel = state.paymentApps.firstOrNull { it.id == state.selectedPaymentAppId }?.label
                ?: state.selectedPaymentAppId,
            highlighted = true,
            options = state.paymentApps.map { it.id to it.label },
            selectedId = state.selectedPaymentAppId,
            onSelect = onSelectPaymentApp,
        )
        Spacer(Modifier.height(12.dp))

        // Account / card
        DropdownField(
            icon = Icons.Filled.AccountBalance,
            label = stringResource(R.string.share_receipt_field_account),
            selectedLabel = state.paymentSources.firstOrNull { it.id == state.selectedAccountId }?.displayName
                ?: stringResource(R.string.share_receipt_account_placeholder),
            highlighted = state.selectedAccountId != null,
            options = state.paymentSources.map { it.id to it.displayName },
            selectedId = state.selectedAccountId,
            onSelect = onSelectAccount,
        )
        Spacer(Modifier.height(12.dp))

        // Category — opens the full hierarchical picker sheet
        val selectedCategoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name
            ?: stringResource(R.string.share_receipt_category_none)
        FieldTile(
            icon = Icons.Filled.Category,
            label = stringResource(R.string.share_receipt_field_category),
            value = selectedCategoryName,
            onClick = { showCategoryPicker = true },
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = state.selectedAccountId != null && parsedAmount != null && parsedAmount > 0 && !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (parsedAmount != null && parsedAmount > 0) {
                        stringResource(R.string.share_receipt_save_with_amount, IndianNumberFormat.format(parsedAmount))
                    } else {
                        stringResource(R.string.share_receipt_save)
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAddManually,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) { Text(stringResource(R.string.share_receipt_add_manually)) }
        Spacer(Modifier.height(16.dp))
    }

    if (showDatePicker) {
        ArthaDatePickerDialog(
            initialMillis = state.dateTimeMillis,
            onConfirm = { picked ->
                // Keep the current time-of-day, swap the date.
                val current = Instant.fromEpochMilliseconds(state.dateTimeMillis).toLocalDateTime(TimeZone.currentSystemDefault())
                onDateTimeChange(mergeTimeKeepingDate(current.hour, current.minute, picked))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        ArthaTimePickerDialog(
            initialMillis = state.dateTimeMillis,
            onConfirm = { h, m -> onDateTimeChange(mergeTimeKeepingDate(h, m, state.dateTimeMillis)); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = state.categories,
            type = CategoryType.EXPENSE,
            onSelected = { onSelectCategory(it.id); showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        color = ExpenseSoft,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Expense.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.share_receipt_amount_label).uppercase(), style = EyebrowStyle, color = Text3)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("₹", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Expense)
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("""^\d{0,10}(\.\d{0,2})?$"""))) onValueChange(new) },
                    placeholder = {
                        Text(
                            stringResource(R.string.share_receipt_amount_placeholder),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
                            color = Text3,
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Expense),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Expense.copy(alpha = 0.6f),
                        unfocusedBorderColor = Expense.copy(alpha = 0.2f),
                        focusedContainerColor = ExpenseSoft,
                        unfocusedContainerColor = ExpenseSoft,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LabeledTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal500, focusedLabelColor = Teal500),
    )
}

@Composable
private fun FieldTile(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Teal500, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label.uppercase(), style = EyebrowStyle, color = Text3)
                Spacer(Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun DropdownField(
    icon: ImageVector,
    label: String,
    selectedLabel: String,
    highlighted: Boolean,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label.uppercase(), style = EyebrowStyle, color = Text3)
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (highlighted) Teal500.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = options.isNotEmpty()) { expanded = true },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if (highlighted) Teal500 else Text3, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (highlighted) MaterialTheme.colorScheme.onSurface else Text3,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = Text3, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        trailingIcon = if (id == selectedId) ({
                            Icon(Icons.Filled.CheckCircle, null, tint = Income, modifier = Modifier.size(16.dp))
                        }) else null,
                        onClick = { onSelect(id); expanded = false },
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val t = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d".format(t.hour, t.minute)
}
