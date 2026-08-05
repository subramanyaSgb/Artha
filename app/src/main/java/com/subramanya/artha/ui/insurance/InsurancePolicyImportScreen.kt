package com.subramanya.artha.ui.insurance

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.ui.common.ArthaDatePickerDialog
import com.subramanya.artha.ui.common.ArthaTextField
import com.subramanya.artha.ui.common.FieldRow
import com.subramanya.artha.ui.common.GhostButton
import com.subramanya.artha.ui.common.InlineTopBar
import com.subramanya.artha.ui.common.PillOption
import com.subramanya.artha.ui.common.PillRadio
import com.subramanya.artha.ui.common.SavePrimaryButton
import com.subramanya.artha.ui.common.SheetChip
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.PolicyDocParser

@Composable
fun InsurancePolicyImportScreen(
    imageUriString: String,
    onBack: () -> Unit,
    onSaved: (insuranceId: String) -> Unit,
    onAddManually: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InsurancePolicyImportViewModel = viewModel(
        factory = InsurancePolicyImportViewModelFactory(
            imageUri = Uri.parse(imageUriString),
            policyDocParser = PolicyDocParser(
                groqKeyProvider = { app.groqApiKey() },
                routesMeKeyProvider = { app.routesMeApiKey() },
                nimKeyProvider = { app.nimApiKey() },
                openRouterKeyProvider = { app.openRouterApiKey() },
            ),
            insuranceRepository = app.insuranceRepository,
            insuranceTypeRepository = app.insuranceTypeRepository,
            context = context,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        (state as? InsurancePolicyImportUiState.Saved)?.let { onSaved(it.insuranceId) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        InlineTopBar(title = stringResource(R.string.policy_import_title), onBack = onBack)
        when (val s = state) {
            is InsurancePolicyImportUiState.Scanning, is InsurancePolicyImportUiState.Saved -> ScanningContent()
            is InsurancePolicyImportUiState.ScanError -> ErrorContent(s.message, vm::retry, onAddManually)
            is InsurancePolicyImportUiState.Parsed -> ParsedContent(
                state = s,
                onNameChange = vm::updateName,
                onSelectType = vm::selectType,
                onProviderChange = vm::updateProvider,
                onPolicyNumberChange = vm::updatePolicyNumber,
                onSumAssuredChange = vm::updateSumAssured,
                onPremiumChange = vm::updatePremium,
                onSelectFrequency = vm::selectFrequency,
                onStartChange = vm::updateStart,
                onEndChange = vm::updateEnd,
                onNextDueChange = vm::updateNextDue,
                onNomineeChange = vm::updateNominee,
                onTaxSectionChange = vm::updateTaxSection,
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
                text = stringResource(R.string.policy_import_scanning),
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
            text = stringResource(R.string.policy_import_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = Text3, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        SavePrimaryButton(label = stringResource(R.string.policy_import_retry), onClick = onRetry)
        Spacer(Modifier.height(12.dp))
        GhostButton(label = stringResource(R.string.policy_import_add_manually), onClick = onAddManually)
    }
}

@Composable
private fun ParsedContent(
    state: InsurancePolicyImportUiState.Parsed,
    onNameChange: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onProviderChange: (String) -> Unit,
    onPolicyNumberChange: (String) -> Unit,
    onSumAssuredChange: (String) -> Unit,
    onPremiumChange: (String) -> Unit,
    onSelectFrequency: (PremiumFrequency) -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long?) -> Unit,
    onNextDueChange: (Long?) -> Unit,
    onNomineeChange: (String) -> Unit,
    onTaxSectionChange: (String) -> Unit,
    onSave: () -> Unit,
    onAddManually: () -> Unit,
) {
    // Text fields hold local state (init once) so fast typing never jumbles against the VM flow.
    var name by remember { mutableStateOf(state.name) }
    var provider by remember { mutableStateOf(state.provider) }
    var policyNumber by remember { mutableStateOf(state.policyNumberText) }
    var sumAssured by remember { mutableStateOf(state.sumAssuredText) }
    var premium by remember { mutableStateOf(state.premiumText) }
    var nominee by remember { mutableStateOf(state.nominee) }
    var taxSection by remember { mutableStateOf(state.taxSection) }

    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var pickingDue by remember { mutableStateOf(false) }

    val typeOptions = state.insuranceTypes.map { PillOption(it.id, it.label) }
    val freqOptions = listOf(
        PillOption(PremiumFrequency.MONTHLY, stringResource(R.string.premium_frequency_monthly)),
        PillOption(PremiumFrequency.QUARTERLY, stringResource(R.string.premium_frequency_quarterly)),
        PillOption(PremiumFrequency.HALF_YEARLY, stringResource(R.string.premium_frequency_half_yearly)),
        PillOption(PremiumFrequency.YEARLY, stringResource(R.string.premium_frequency_yearly)),
        PillOption(PremiumFrequency.SINGLE, stringResource(R.string.premium_frequency_single)),
    )

    val isValid = name.isNotBlank() && provider.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        FieldRow(label = stringResource(R.string.insurance_form_name_label)) {
            ArthaTextField(
                value = name,
                onValueChange = { name = it; onNameChange(it) },
                placeholder = stringResource(R.string.insurance_form_name_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_type_label)) {
            PillRadio(value = state.typeId, options = typeOptions, onChange = onSelectType)
        }
        FieldRow(label = stringResource(R.string.insurance_form_provider_label)) {
            ArthaTextField(
                value = provider,
                onValueChange = { provider = it; onProviderChange(it) },
                placeholder = stringResource(R.string.insurance_form_provider_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_policy_number_label), optional = true) {
            ArthaTextField(
                value = policyNumber,
                onValueChange = { policyNumber = it; onPolicyNumberChange(it) },
                placeholder = stringResource(R.string.insurance_form_policy_number_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_sum_assured_label)) {
            ArthaTextField(
                value = sumAssured,
                onValueChange = { v ->
                    val cleaned = v.filterIndexed { i, c -> c.isDigit() || (c == '.' && v.indexOf('.') == i) }
                    sumAssured = cleaned; onSumAssuredChange(cleaned)
                },
                placeholder = stringResource(R.string.insurance_form_sum_assured_placeholder),
                suffix = "₹",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_premium_label)) {
            ArthaTextField(
                value = premium,
                onValueChange = { v ->
                    val cleaned = v.filterIndexed { i, c -> c.isDigit() || (c == '.' && v.indexOf('.') == i) }
                    premium = cleaned; onPremiumChange(cleaned)
                },
                placeholder = stringResource(R.string.insurance_form_premium_placeholder),
                suffix = "₹",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_frequency_label)) {
            PillRadio(value = state.frequency, options = freqOptions, onChange = onSelectFrequency)
        }
        FieldRow(label = stringResource(R.string.insurance_form_start_label)) {
            SheetChip(
                label = DateFormatter.longDate(state.startMillis),
                leading = Icons.Filled.CalendarMonth,
                onClick = { pickingStart = true },
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_next_due_label), optional = true) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetChip(
                    label = state.nextDueMillis?.let { DateFormatter.longDate(it) }
                        ?: stringResource(R.string.insurance_form_date_pick_cta),
                    leading = Icons.Filled.CalendarMonth,
                    onClick = { pickingDue = true },
                )
                if (state.nextDueMillis != null) {
                    TextButton(onClick = { onNextDueChange(null) }) {
                        Text(stringResource(R.string.insurance_form_clear_due))
                    }
                }
            }
        }
        FieldRow(label = stringResource(R.string.insurance_form_end_label), optional = true) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetChip(
                    label = state.endMillis?.let { DateFormatter.longDate(it) }
                        ?: stringResource(R.string.insurance_form_date_pick_cta),
                    leading = Icons.Filled.CalendarMonth,
                    onClick = { pickingEnd = true },
                )
                if (state.endMillis != null) {
                    TextButton(onClick = { onEndChange(null) }) {
                        Text(stringResource(R.string.insurance_form_clear_end))
                    }
                }
            }
        }
        FieldRow(label = stringResource(R.string.insurance_form_nominee_label), optional = true) {
            ArthaTextField(
                value = nominee,
                onValueChange = { nominee = it; onNomineeChange(it) },
                placeholder = stringResource(R.string.insurance_form_nominee_placeholder),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
        }
        FieldRow(label = stringResource(R.string.insurance_form_tax_section_label), optional = true) {
            ArthaTextField(
                value = taxSection,
                onValueChange = { taxSection = it.uppercase(); onTaxSectionChange(it.uppercase()) },
                placeholder = stringResource(R.string.insurance_form_tax_section_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }

        // Rich extras (members/riders/coverage/…) are captured verbatim into details_json and
        // rendered on the detail screen (Task 7). Here we just tell the user they were captured.
        if (state.extraDetailCount > 0) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.policy_import_extra_details, state.extraDetailCount),
                style = MaterialTheme.typography.bodySmall,
                color = Text3,
            )
        }

        Spacer(Modifier.height(28.dp))
        SavePrimaryButton(
            label = stringResource(R.string.policy_import_save),
            enabled = isValid && !state.isSaving,
            onClick = onSave,
        )
        Spacer(Modifier.height(12.dp))
        GhostButton(label = stringResource(R.string.policy_import_add_manually), onClick = onAddManually)
        Spacer(Modifier.height(20.dp))
    }

    if (pickingStart) {
        ArthaDatePickerDialog(
            initialMillis = state.startMillis,
            onConfirm = { onStartChange(it); pickingStart = false },
            onDismiss = { pickingStart = false },
        )
    }
    if (pickingDue) {
        ArthaDatePickerDialog(
            initialMillis = state.nextDueMillis ?: System.currentTimeMillis(),
            onConfirm = { onNextDueChange(it); pickingDue = false },
            onDismiss = { pickingDue = false },
        )
    }
    if (pickingEnd) {
        ArthaDatePickerDialog(
            initialMillis = state.endMillis ?: System.currentTimeMillis(),
            onConfirm = { onEndChange(it); pickingEnd = false },
            onDismiss = { pickingEnd = false },
        )
    }
}
