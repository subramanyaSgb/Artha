package com.subramanya.artha.ui.cards

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.CardNetwork
import com.subramanya.artha.data.entity.enums.CardType
import com.subramanya.artha.domain.model.Card
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Combined Add / Edit card sheet. Field visibility flexes with the chosen type:
 *   CREDIT  → name + network + issuer + last4 + creditLimit + statementDay + dueDay
 *   DEBIT   → name + network + issuer + last4 + linkedAccountId
 *   PREPAID → name + network + issuer + last4
 *
 * Validation:
 *   - name required
 *   - creditLimit > 0 for CREDIT
 *   - statementDay, dueDay in 1..31 for CREDIT
 *   - last4 either empty OR exactly 4 digits
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardFormSheet(
    editing: Card?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accounts by app.accountRepository.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: CardType.CREDIT) }
    var network by remember(editing) { mutableStateOf(editing?.network ?: CardNetwork.VISA) }
    var issuer by remember(editing) { mutableStateOf(editing?.issuer.orEmpty()) }
    var last4 by remember(editing) { mutableStateOf(editing?.cardNumberLast4.orEmpty()) }
    var creditLimitText by remember(editing) {
        mutableStateOf(editing?.creditLimit?.toPlainString() ?: "")
    }
    var statementDayText by remember(editing) {
        mutableStateOf(editing?.statementDayOfMonth?.toString() ?: "")
    }
    var dueDayText by remember(editing) {
        mutableStateOf(editing?.dueDayOfMonth?.toString() ?: "")
    }
    var linkedAccountId by remember(editing) { mutableStateOf(editing?.linkedAccountId) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }
    var showErrors by remember { mutableStateOf(false) }

    val parsedLimit = creditLimitText.toDoubleOrNull()
    val parsedStatement = statementDayText.toIntOrNull()
    val parsedDue = dueDayText.toIntOrNull()
    val last4Valid = last4.isEmpty() || (last4.length == 4 && last4.all { it.isDigit() })

    val isValid: Boolean = name.isNotBlank() && last4Valid && when (type) {
        CardType.CREDIT -> (parsedLimit != null && parsedLimit > 0.0) &&
            parsedStatement in 1..31 && parsedDue in 1..31
        CardType.DEBIT, CardType.PREPAID -> true
    }

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
                    if (editing == null) R.string.card_form_add_title else R.string.card_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.card_form_name_label)) },
                placeholder = { Text(stringResource(R.string.card_form_name_placeholder)) },
                isError = showErrors && name.isBlank(),
                supportingText = {
                    if (showErrors && name.isBlank()) {
                        Text(stringResource(R.string.card_form_validation_name))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.card_form_type_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.displayLabel()) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.card_form_network_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardNetwork.entries.forEach { option ->
                    FilterChip(
                        selected = network == option,
                        onClick = { network = option },
                        label = { Text(option.displayLabel()) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = issuer,
                onValueChange = { issuer = it },
                singleLine = true,
                label = { Text(stringResource(R.string.card_form_issuer_label)) },
                placeholder = { Text(stringResource(R.string.card_form_issuer_placeholder)) },
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
                label = { Text(stringResource(R.string.card_form_last4_label)) },
                isError = showErrors && !last4Valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            // Credit-card-only fields
            if (type == CardType.CREDIT) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { v ->
                        creditLimitText = v.filterIndexed { i, c ->
                            c.isDigit() || (c == '.' && v.indexOf('.') == i)
                        }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.card_form_credit_limit_label)) },
                    prefix = { Text("₹") },
                    isError = showErrors && (parsedLimit == null || parsedLimit <= 0.0),
                    supportingText = {
                        if (showErrors && (parsedLimit == null || parsedLimit <= 0.0)) {
                            Text(stringResource(R.string.card_form_validation_credit_limit))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = statementDayText,
                    onValueChange = { v -> statementDayText = v.filter { it.isDigit() }.take(2) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.card_form_statement_day_label)) },
                    isError = showErrors && parsedStatement !in 1..31,
                    supportingText = {
                        if (showErrors && parsedStatement !in 1..31) {
                            Text(stringResource(R.string.card_form_validation_statement_day))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { v -> dueDayText = v.filter { it.isDigit() }.take(2) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.card_form_due_day_label)) },
                    isError = showErrors && parsedDue !in 1..31,
                    supportingText = {
                        if (showErrors && parsedDue !in 1..31) {
                            Text(stringResource(R.string.card_form_validation_due_day))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Debit-only field
            if (type == CardType.DEBIT) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.card_form_linked_account_label), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.card_form_linked_account_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { acct ->
                            FilterChip(
                                selected = linkedAccountId == acct.id,
                                onClick = { linkedAccountId = acct.id },
                                label = { Text(acct.name) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.card_form_color_label), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.forEach { swatch ->
                    val ringWidth = if (color == swatch) 3.dp else 0.dp
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(swatch))
                            .border(width = ringWidth, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                            .clickable { color = swatch },
                    ) { /* purely visual */ }
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
                    val resolved = Card(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        issuer = issuer.trim().takeIf { it.isNotBlank() },
                        network = network,
                        cardNumberLast4 = last4.takeIf { it.length == 4 },
                        creditLimit = parsedLimit?.takeIf { type == CardType.CREDIT },
                        statementDayOfMonth = parsedStatement?.takeIf { type == CardType.CREDIT },
                        dueDayOfMonth = parsedDue?.takeIf { type == CardType.CREDIT },
                        linkedAccountId = linkedAccountId?.takeIf { type == CardType.DEBIT },
                        icon = "credit_card",
                        color = color,
                        isArchived = editing?.isArchived ?: false,
                        displayOrder = editing?.displayOrder ?: nextDisplayOrder(),
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.cardRepository.upsert(resolved)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.card_form_save)) }
        }
    }
}

@Composable
private fun CardType.displayLabel(): String = when (this) {
    CardType.CREDIT -> stringResource(R.string.card_form_type_credit)
    CardType.DEBIT -> stringResource(R.string.card_form_type_debit)
    CardType.PREPAID -> stringResource(R.string.card_form_type_prepaid)
}

@Composable
private fun CardNetwork.displayLabel(): String = when (this) {
    CardNetwork.VISA -> stringResource(R.string.card_form_network_visa)
    CardNetwork.MASTERCARD -> stringResource(R.string.card_form_network_mastercard)
    CardNetwork.RUPAY -> stringResource(R.string.card_form_network_rupay)
    CardNetwork.AMEX -> stringResource(R.string.card_form_network_amex)
    CardNetwork.DINERS -> stringResource(R.string.card_form_network_diners)
}

private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private val PALETTE: List<Long> = listOf(
    0xFF1F2937L, // slate-800 (matte black-ish)
    0xFF0F766EL, // teal-700
    0xFF4338CAL, // indigo-700
    0xFFBE185DL, // pink-700
    0xFFB45309L, // amber-700
    0xFF6D28D9L, // violet-700
)
