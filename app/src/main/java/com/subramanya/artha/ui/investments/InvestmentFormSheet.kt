package com.subramanya.artha.ui.investments

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.subramanya.artha.data.entity.enums.InvestmentType
import com.subramanya.artha.domain.model.Investment
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Add / Edit Investment sheet. Type drives which optional fields surface:
 *   - FD / RD / Bonds → maturity date matters; units/NAV usually don't.
 *   - MUTUAL_FUND / EQUITY / SIP / Gold-Digital → units + NAV.
 *   - PPF / EPF / NPS / ULIP / OTHER → just core fields; let the user fill
 *     whatever applies (we don't enforce).
 *
 * Tax-section field is opt-in text — '80C', '80CCD(1B)', etc. — so the user
 * doesn't have to remember an enum.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InvestmentFormSheet(
    editing: Investment?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var type by remember(editing) { mutableStateOf(editing?.type ?: InvestmentType.FD) }
    var institution by remember(editing) { mutableStateOf(editing?.institution.orEmpty()) }
    var currentValueText by remember(editing) {
        mutableStateOf(editing?.currentValue?.toPlainString() ?: "")
    }
    var unitsText by remember(editing) {
        mutableStateOf(editing?.units?.toPlainString() ?: "")
    }
    var navText by remember(editing) {
        mutableStateOf(editing?.nav?.toPlainString() ?: "")
    }
    var startDate by remember(editing) {
        mutableStateOf(editing?.startDate ?: System.currentTimeMillis())
    }
    var maturityDate by remember(editing) { mutableStateOf(editing?.maturityDate) }
    var taxSection by remember(editing) { mutableStateOf(editing?.taxSection.orEmpty()) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: PALETTE.first()) }

    var showErrors by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingMaturity by remember { mutableStateOf(false) }

    val parsedCurrentValue = remember(currentValueText) {
        if (currentValueText.isBlank()) 0.0 else currentValueText.toDoubleOrNull()
    }
    val parsedUnits = remember(unitsText) {
        if (unitsText.isBlank()) null else unitsText.toDoubleOrNull()
    }
    val parsedNav = remember(navText) {
        if (navText.isBlank()) null else navText.toDoubleOrNull()
    }
    val isValid = name.isNotBlank() && parsedCurrentValue != null

    val showUnitsAndNav = type in setOf(
        InvestmentType.SIP, InvestmentType.MUTUAL_FUND, InvestmentType.EQUITY,
        InvestmentType.GOLD_DIGITAL,
    )
    val showMaturity = type in setOf(InvestmentType.FD, InvestmentType.RD, InvestmentType.BONDS)

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
                    if (editing == null) R.string.investment_form_add_title
                    else R.string.investment_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.investment_form_name_label)) },
                placeholder = { Text(stringResource(R.string.investment_form_name_placeholder)) },
                isError = showErrors && name.isBlank(),
                supportingText = {
                    if (showErrors && name.isBlank()) {
                        Text(stringResource(R.string.investment_form_validation_name))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.investment_form_type_label), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InvestmentType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                singleLine = true,
                label = { Text(stringResource(R.string.investment_form_institution_label)) },
                placeholder = { Text(stringResource(R.string.investment_form_institution_placeholder)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = currentValueText,
                onValueChange = { v ->
                    currentValueText = v.filterIndexed { i, c ->
                        c.isDigit() || (c == '.' && v.indexOf('.') == i)
                    }
                },
                singleLine = true,
                label = { Text(stringResource(R.string.investment_form_current_value_label)) },
                prefix = { Text("₹") },
                isError = showErrors && parsedCurrentValue == null,
                supportingText = {
                    if (showErrors && parsedCurrentValue == null) {
                        Text(stringResource(R.string.investment_form_validation_amount))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            if (showUnitsAndNav) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { v ->
                        unitsText = v.filterIndexed { i, c ->
                            c.isDigit() || (c == '.' && v.indexOf('.') == i)
                        }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.investment_form_units_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = navText,
                    onValueChange = { v ->
                        navText = v.filterIndexed { i, c ->
                            c.isDigit() || (c == '.' && v.indexOf('.') == i)
                        }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.investment_form_nav_label)) },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
            DateField(
                label = stringResource(R.string.investment_form_start_date_label),
                epochMillis = startDate,
                onClick = { pickingStart = true },
            )

            if (showMaturity) {
                Spacer(Modifier.height(16.dp))
                DateField(
                    label = stringResource(R.string.investment_form_maturity_date_label),
                    epochMillis = maturityDate,
                    onClick = { pickingMaturity = true },
                )
                if (maturityDate != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { maturityDate = null }) {
                        Text(stringResource(R.string.investment_form_clear_maturity))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = taxSection,
                onValueChange = { taxSection = it.uppercase() },
                singleLine = true,
                label = { Text(stringResource(R.string.investment_form_tax_section_label)) },
                placeholder = { Text(stringResource(R.string.investment_form_tax_section_placeholder)) },
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
                    val resolved = Investment(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        type = type,
                        institution = institution.trim().takeIf { it.isNotBlank() },
                        currentValue = parsedCurrentValue ?: 0.0,
                        units = if (showUnitsAndNav) parsedUnits else editing?.units,
                        nav = if (showUnitsAndNav) parsedNav else editing?.nav,
                        startDate = startDate,
                        maturityDate = if (showMaturity) maturityDate else editing?.maturityDate,
                        taxSection = taxSection.trim().takeIf { it.isNotBlank() },
                        icon = editing?.icon ?: "savings",
                        color = color,
                        linkedInsuranceId = editing?.linkedInsuranceId,
                        isArchived = editing?.isArchived ?: false,
                        displayOrder = editing?.displayOrder ?: nextDisplayOrder(),
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.investmentRepository.upsert(resolved)
                        onDismiss()
                    }
                },
                enabled = !showErrors || isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.investment_form_save)) }
        }
    }

    if (pickingStart) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { startDate = it }
                    pickingStart = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (pickingMaturity) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = maturityDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { pickingMaturity = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { maturityDate = it }
                    pickingMaturity = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingMaturity = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
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
private fun InvestmentType.displayName(): String = when (this) {
    InvestmentType.FD -> stringResource(R.string.investment_type_fd)
    InvestmentType.RD -> stringResource(R.string.investment_type_rd)
    InvestmentType.SIP -> stringResource(R.string.investment_type_sip)
    InvestmentType.MUTUAL_FUND -> stringResource(R.string.investment_type_mutual_fund)
    InvestmentType.EQUITY -> stringResource(R.string.investment_type_equity)
    InvestmentType.GOLD_PHYSICAL -> stringResource(R.string.investment_type_gold_physical)
    InvestmentType.GOLD_DIGITAL -> stringResource(R.string.investment_type_gold_digital)
    InvestmentType.BONDS -> stringResource(R.string.investment_type_bonds)
    InvestmentType.PPF -> stringResource(R.string.investment_type_ppf)
    InvestmentType.EPF -> stringResource(R.string.investment_type_epf)
    InvestmentType.NPS -> stringResource(R.string.investment_type_nps)
    InvestmentType.ULIP -> stringResource(R.string.investment_type_ulip)
    InvestmentType.OTHER -> stringResource(R.string.investment_type_other)
}

private fun nextDisplayOrder(): Int = (System.currentTimeMillis() / 1000L).toInt()

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
