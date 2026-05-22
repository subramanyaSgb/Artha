package com.subramanya.artha.ui.insurance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.subramanya.artha.data.entity.enums.InsuranceType
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Add/Edit Insurance bottom sheet.
 *
 * PRD §7.12 calls for a linked Investment row for endowment/ULIP policies — when
 * the type matches and the linkage switch is on, we also mint an InvestmentEntity
 * with `linkedInsuranceId` pointing back at this policy. The user can break the
 * link later by editing either side.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InsuranceFormSheet(
    editing: Insurance?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: InsuranceType.HEALTH) }
    var provider by remember(editing) { mutableStateOf(editing?.provider.orEmpty()) }
    var policyNumber by remember(editing) { mutableStateOf(editing?.policyNumber.orEmpty()) }
    var sumAssuredText by remember(editing) {
        mutableStateOf(editing?.sumAssured?.toPlainString() ?: "")
    }
    var premiumText by remember(editing) {
        mutableStateOf(editing?.premiumAmount?.toPlainString() ?: "")
    }
    var frequency by remember(editing) {
        mutableStateOf(editing?.premiumFrequency ?: PremiumFrequency.YEARLY)
    }
    var startDate by remember(editing) {
        mutableStateOf(editing?.startDate ?: System.currentTimeMillis())
    }
    var endDate by remember(editing) { mutableStateOf(editing?.endDate) }
    var nextDueDate by remember(editing) { mutableStateOf(editing?.nextPremiumDate) }
    var nominee by remember(editing) { mutableStateOf(editing?.nominee.orEmpty()) }
    var agent by remember(editing) { mutableStateOf(editing?.agentContact.orEmpty()) }
    var taxSection by remember(editing) {
        mutableStateOf(editing?.taxSection.orEmpty())
    }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }

    // Only relevant on new endowment rows; not surfaced on edit because the
    // linked investment already exists (or doesn't). InsuranceType.LIFE_ENDOWMENT
    // covers both endowment and ULIP policies on the insurance side; the linked
    // Investment we create is typed as InvestmentType.ULIP since that's the
    // closest cost-basis-tracking match in the seed taxonomy.
    var createLinkedInvestment by remember { mutableStateOf(false) }

    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var pickingDue by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val parsedSum = sumAssuredText.toDoubleOrNull()
    val parsedPremium = premiumText.toDoubleOrNull()
    val isValid =
        name.isNotBlank() && provider.isNotBlank() && parsedSum != null && parsedPremium != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.subramanya.artha.ui.theme.Surface3,
        contentWindowInsets = com.subramanya.artha.ui.common.SheetWindowInsets,
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
                    if (editing == null) R.string.insurance_form_add_title
                    else R.string.insurance_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_name_label)) },
                placeholder = { Text(stringResource(R.string.insurance_form_name_placeholder)) },
                isError = showErrors && name.isBlank(),
                supportingText = {
                    if (showErrors && name.isBlank()) {
                        Text(stringResource(R.string.insurance_form_validation_name))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.insurance_form_type_label), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsuranceType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.displayName()) },
                    )
                }
            }

            // Linked-investment switch shown only for endowment on a NEW row.
            if (editing == null && type == InsuranceType.LIFE_ENDOWMENT) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.insurance_form_link_investment_title),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.insurance_form_link_investment_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = createLinkedInvestment,
                        onCheckedChange = { createLinkedInvestment = it },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_provider_label)) },
                placeholder = { Text(stringResource(R.string.insurance_form_provider_placeholder)) },
                isError = showErrors && provider.isBlank(),
                supportingText = {
                    if (showErrors && provider.isBlank()) {
                        Text(stringResource(R.string.insurance_form_validation_provider))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = policyNumber,
                onValueChange = { policyNumber = it },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_policy_number_label)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = sumAssuredText,
                onValueChange = { v ->
                    sumAssuredText = v.filterIndexed { i, c ->
                        c.isDigit() || (c == '.' && v.indexOf('.') == i)
                    }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_sum_assured_label)) },
                prefix = { Text("₹") },
                isError = showErrors && parsedSum == null,
                supportingText = {
                    if (showErrors && parsedSum == null) {
                        Text(stringResource(R.string.insurance_form_validation_amount))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = premiumText,
                onValueChange = { v ->
                    premiumText = v.filterIndexed { i, c ->
                        c.isDigit() || (c == '.' && v.indexOf('.') == i)
                    }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_premium_label)) },
                prefix = { Text("₹") },
                isError = showErrors && parsedPremium == null,
                supportingText = {
                    if (showErrors && parsedPremium == null) {
                        Text(stringResource(R.string.insurance_form_validation_amount))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.insurance_form_frequency_label), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumFrequency.entries.forEach { option ->
                    FilterChip(
                        selected = frequency == option,
                        onClick = { frequency = option },
                        label = { Text(option.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            DateField(
                label = stringResource(R.string.insurance_form_start_label),
                epochMillis = startDate,
                onClick = { pickingStart = true },
            )

            Spacer(Modifier.height(16.dp))
            DateField(
                label = stringResource(R.string.insurance_form_next_due_label),
                epochMillis = nextDueDate,
                onClick = { pickingDue = true },
            )
            if (nextDueDate != null) {
                TextButton(onClick = { nextDueDate = null }) {
                    Text(stringResource(R.string.insurance_form_clear_due))
                }
            }

            Spacer(Modifier.height(16.dp))
            DateField(
                label = stringResource(R.string.insurance_form_end_label),
                epochMillis = endDate,
                onClick = { pickingEnd = true },
            )
            if (endDate != null) {
                TextButton(onClick = { endDate = null }) {
                    Text(stringResource(R.string.insurance_form_clear_end))
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = nominee,
                onValueChange = { nominee = it },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_nominee_label)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = agent,
                onValueChange = { agent = it },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_agent_label)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = taxSection,
                onValueChange = { taxSection = it.uppercase() },
                singleLine = true,
                label = { Text(stringResource(R.string.insurance_form_tax_section_label)) },
                placeholder = { Text(stringResource(R.string.insurance_form_tax_section_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.investment_form_color_label), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PALETTE.forEach { swatch ->
                    ColorSwatch(
                        color = Color(swatch),
                        selected = color == swatch,
                        onClick = { color = swatch },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val resolvedId = editing?.id ?: UUID.randomUUID().toString()
                    // Default tax-section by type for the convenience of FY-end tracking.
                    val resolvedTax = taxSection.trim().takeIf { it.isNotBlank() }
                        ?: defaultTaxSectionFor(type)
                    val resolved = Insurance(
                        id = resolvedId,
                        name = name.trim(),
                        type = type,
                        provider = provider.trim(),
                        policyNumber = policyNumber.trim().takeIf { it.isNotBlank() },
                        sumAssured = parsedSum ?: 0.0,
                        premiumAmount = parsedPremium ?: 0.0,
                        premiumFrequency = frequency,
                        nextPremiumDate = nextDueDate,
                        startDate = startDate,
                        endDate = endDate,
                        nominee = nominee.trim().takeIf { it.isNotBlank() },
                        agentContact = agent.trim().takeIf { it.isNotBlank() },
                        policyDocUri = editing?.policyDocUri,
                        taxSection = resolvedTax,
                        icon = editing?.icon ?: "shield",
                        color = color,
                        isArchived = editing?.isArchived ?: false,
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.insuranceRepository.upsert(resolved)
                        // Side-effect: spin up a linked Investment so endowment/ULIP gains
                        // proper cost-basis tracking. The Investment row links back via
                        // linkedInsuranceId; user can also break the link later.
                        if (editing == null && createLinkedInvestment) {
                            val linkedId = UUID.randomUUID().toString()
                            app.investmentRepository.upsert(
                                Investment(
                                    id = linkedId,
                                    name = name.trim(),
                                    type = InvestmentType.ULIP,
                                    institution = provider.trim().takeIf { it.isNotBlank() },
                                    currentValue = 0.0,
                                    units = null,
                                    nav = null,
                                    startDate = startDate,
                                    maturityDate = endDate,
                                    taxSection = resolvedTax,
                                    icon = "savings",
                                    color = color,
                                    linkedInsuranceId = resolvedId,
                                    isArchived = false,
                                    displayOrder = (now / 1000).toInt(),
                                    createdAt = now,
                                ),
                            )
                        }
                        onDismiss()
                    }
                },
                enabled = !showErrors || isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.insurance_form_save)) }
        }
    }

    if (pickingStart) {
        DatePickerSheet(
            initialEpoch = startDate,
            onConfirm = { startDate = it; pickingStart = false },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingDue) {
        DatePickerSheet(
            initialEpoch = nextDueDate ?: System.currentTimeMillis(),
            onConfirm = { nextDueDate = it; pickingDue = false },
            onDismiss = { pickingDue = false },
        )
    }
    if (pickingEnd) {
        DatePickerSheet(
            initialEpoch = endDate ?: System.currentTimeMillis(),
            onConfirm = { endDate = it; pickingEnd = false },
            onDismiss = { pickingEnd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialEpoch: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let(onConfirm) ?: onDismiss()
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    ) { DatePicker(state = pickerState) }
}

@Composable
private fun DateField(label: String, epochMillis: Long?, onClick: () -> Unit) {
    OutlinedTextField(
        value = epochMillis?.let { DateFormatter.longDate(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.investment_form_pick_date)) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        enabled = false,
    )
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
    ) { }
}

@Composable
private fun PremiumFrequency.displayName(): String = when (this) {
    PremiumFrequency.MONTHLY -> stringResource(R.string.premium_frequency_monthly)
    PremiumFrequency.QUARTERLY -> stringResource(R.string.premium_frequency_quarterly)
    PremiumFrequency.HALF_YEARLY -> stringResource(R.string.premium_frequency_half_yearly)
    PremiumFrequency.YEARLY -> stringResource(R.string.premium_frequency_yearly)
    PremiumFrequency.SINGLE -> stringResource(R.string.premium_frequency_single)
}

private fun defaultTaxSectionFor(type: InsuranceType): String? = when (type) {
    InsuranceType.HEALTH -> "80D"
    InsuranceType.LIFE_TERM, InsuranceType.LIFE_ENDOWMENT -> "80C"
    else -> null
}

private fun Double.toPlainString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private val PALETTE: List<Long> = listOf(
    0xFF0F766EL,
    0xFF4338CAL,
    0xFF15803DL,
    0xFFB45309L,
    0xFFBE185DL,
    0xFF6D28D9L,
)
