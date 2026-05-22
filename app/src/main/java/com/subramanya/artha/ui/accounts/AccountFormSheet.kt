package com.subramanya.artha.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.AccountType
import com.subramanya.artha.domain.model.Account
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Combined Add / Edit modal sheet. When [editing] is non-null we hydrate the form
 * with its values and `save()` calls upsert; otherwise we mint a new UUID.
 *
 * Icon / color come from a fixed Phase-1 palette — full custom pickers are Session 10.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountFormSheet(
    editing: Account?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: AccountType.SAVINGS) }
    var institution by remember(editing) { mutableStateOf(editing?.institution.orEmpty()) }
    var last4 by remember(editing) { mutableStateOf(editing?.accountNumberLast4.orEmpty()) }
    var openingText by remember(editing) {
        mutableStateOf(editing?.openingBalance?.toPlainString() ?: "")
    }
    var icon by remember(editing) { mutableStateOf(editing?.icon ?: "account_balance") }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }

    var showErrors by remember { mutableStateOf(false) }
    val parsedBalance = remember(openingText) {
        if (openingText.isBlank()) 0.0 else openingText.toDoubleOrNull()
    }
    val last4Valid = last4.isEmpty() || (last4.length == 4 && last4.all { it.isDigit() })
    val isValid = name.isNotBlank() && parsedBalance != null && last4Valid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        dragHandle = { com.subramanya.artha.ui.common.ArthaSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(
                    if (editing == null) R.string.account_form_add_title else R.string.account_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.account_form_name_label)) },
                placeholder = { Text(stringResource(R.string.account_form_name_placeholder)) },
                isError = showErrors && name.isBlank(),
                supportingText = {
                    if (showErrors && name.isBlank()) {
                        Text(stringResource(R.string.account_form_validation_name))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.account_form_type_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.displayName()) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                singleLine = true,
                label = { Text(stringResource(R.string.account_form_institution_label)) },
                placeholder = { Text(stringResource(R.string.account_form_institution_placeholder)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = last4,
                onValueChange = { v -> last4 = v.filter { it.isDigit() }.take(4) },
                singleLine = true,
                label = { Text(stringResource(R.string.account_form_last4_label)) },
                isError = showErrors && !last4Valid,
                supportingText = {
                    if (showErrors && !last4Valid) {
                        Text(stringResource(R.string.account_form_validation_last4))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = openingText,
                onValueChange = { v ->
                    openingText = v.filterIndexed { i, c ->
                        c.isDigit() || (c == '.' && v.indexOf('.') == i)
                    }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.account_form_opening_label)) },
                prefix = { Text("₹") },
                isError = showErrors && parsedBalance == null,
                supportingText = {
                    if (showErrors && parsedBalance == null) {
                        Text(stringResource(R.string.account_form_validation_balance))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.account_form_color_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.forEach { swatch ->
                    ColorSwatch(
                        color = Color(swatch),
                        selected = color == swatch,
                        onClick = { color = swatch },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.account_form_icon_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ICONS.forEach { iconName ->
                    FilterChip(
                        selected = icon == iconName,
                        onClick = { icon = iconName },
                        label = { Text(iconName.replace('_', ' ')) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val resolved = Account(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        institution = institution.trim().takeIf { it.isNotBlank() },
                        accountNumberLast4 = last4.takeIf { it.length == 4 },
                        openingBalance = parsedBalance ?: 0.0,
                        currency = editing?.currency ?: "INR",
                        icon = icon,
                        color = color,
                        isArchived = editing?.isArchived ?: false,
                        displayOrder = editing?.displayOrder ?: nextDisplayOrder(),
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.accountRepository.upsert(resolved)
                        onDismiss()
                    }
                },
                enabled = !showErrors || isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.account_form_save)) }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ringWidth = if (selected) 3.dp else 0.dp
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = ringWidth, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
            .clickable(onClick = onClick),
    ) { /* swatch is purely visual — no inner content needed */ }
}

@Composable
private fun AccountType.displayName(): String = when (this) {
    AccountType.SAVINGS -> stringResource(R.string.onboarding_account_type_savings)
    AccountType.CURRENT -> stringResource(R.string.onboarding_account_type_current)
    AccountType.CASH -> stringResource(R.string.onboarding_account_type_cash)
    AccountType.WALLET -> stringResource(R.string.onboarding_account_type_wallet)
}

/**
 * Always lands a brand-new account at the bottom of the active list. Time-based key
 * is fine for Phase 1 — the user can drag-reorder anyway and the value fits in Int
 * for the next 68 years.
 */
private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

// Phase 1 palette — six tasteful swatches per the design pass. Keep stored as ARGB Long.
private val PALETTE: List<Long> = listOf(
    0xFF0F766EL, // teal-700 (seed)
    0xFF4338CAL, // indigo-700
    0xFF15803DL, // emerald-700
    0xFFB45309L, // amber-700
    0xFFBE185DL, // pink-700
    0xFF6D28D9L, // violet-700
)

private val ICONS: List<String> = listOf(
    "account_balance",
    "account_balance_wallet",
    "payments",
    "savings",
)
