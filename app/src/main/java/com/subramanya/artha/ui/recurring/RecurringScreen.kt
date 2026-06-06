package com.subramanya.artha.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.RecurringFrequency
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.RecurringRule
import com.subramanya.artha.domain.recurring.RecurringTemplate
import com.subramanya.artha.domain.recurring.RecurringTemplateCodec
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.ui.common.EmptyState
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.IbmPlexMono
import com.subramanya.artha.ui.theme.InstrumentSerif
import com.subramanya.artha.ui.theme.Line1
import com.subramanya.artha.ui.theme.LineTeal
import com.subramanya.artha.ui.theme.Ochre
import com.subramanya.artha.ui.theme.Surface1
import com.subramanya.artha.ui.theme.Surface2
import com.subramanya.artha.ui.theme.Surface4
import com.subramanya.artha.ui.theme.Teal300
import com.subramanya.artha.ui.theme.Teal700
import com.subramanya.artha.ui.theme.Teal900
import com.subramanya.artha.ui.theme.Text1
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val rules by app.recurringRuleRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var formMode: FormMode? by remember { mutableStateOf(null) }
    var pendingDelete: RecurringRule? by remember { mutableStateOf(null) }

    Surface(color = Surface1, modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface1,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { formMode = FormMode.Add },
                    containerColor = Teal700,
                    contentColor = Text1,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.recurring_fab_add)) },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    com.subramanya.artha.ui.common.InlineTopBar(
                        title = stringResource(R.string.recurring_title),
                        onBack = onBack,
                    )
                }
                item { OchreInfoBanner(text = stringResource(R.string.recurring_banner)) }
                if (rules.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(icon = Icons.Filled.EventRepeat, title = stringResource(R.string.recurring_empty))
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        RecurringRow(
                            rule = rule,
                            onTap = { formMode = FormMode.Edit(rule) },
                            onToggle = { active ->
                                scope.launch { app.recurringRuleRepository.upsert(rule.copy(isActive = active)) }
                            },
                            onDelete = { pendingDelete = rule },
                        )
                    }
                }
            }
        }
    }

    val mode = formMode
    if (mode != null) {
        RecurringFormSheet(
            editing = (mode as? FormMode.Edit)?.rule,
            onSave = { resolved -> scope.launch { app.recurringRuleRepository.upsert(resolved); formMode = null } },
            onDismiss = { formMode = null },
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.recurring_delete_confirm_title),
            text = stringResource(R.string.recurring_delete_confirm_body),
            confirmLabel = stringResource(R.string.recurring_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = { scope.launch { app.recurringRuleRepository.delete(toDelete); pendingDelete = null } },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = { pendingDelete = null },
        )
    }
}

private sealed interface FormMode {
    data object Add : FormMode
    data class Edit(val rule: RecurringRule) : FormMode
}

/**
 * HANDOFF §3.7 — Recurring info-banner: ochre 10% fill + ochre 30% border,
 * with an info icon and the verbatim "Rules stored locally … Phase 5" copy.
 */
@Composable
private fun OchreInfoBanner(text: String) {
    Surface(
        color = Ochre.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Ochre.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Ochre,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Text2,
            )
        }
    }
}

@Composable
private fun RecurringRow(
    rule: RecurringRule,
    onTap: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = Surface2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (rule.isActive) LineTeal else Line1,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onTap),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Teal300,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                    ),
                    color = Text1,
                    modifier = Modifier.weight(1f),
                )
                if (rule.autoConfirm) {
                    Surface(
                        color = Teal900,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.recurring_auto_chip).uppercase(),
                            style = TextStyle(
                                fontFamily = IbmPlexMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Teal300,
                                letterSpacing = 0.06.em,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Switch(
                    checked = rule.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Text1,
                        checkedTrackColor = Teal700,
                        uncheckedThumbColor = Text2,
                        uncheckedTrackColor = Surface4,
                        uncheckedBorderColor = Line1,
                    ),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            RecurringTemplateCodec.decode(rule.transactionTemplate)?.let { t ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${IndianNumberFormat.format(t.amount)} · ${t.type.name.lowercase().replaceFirstChar { it.titlecase() }}" +
                        if (t.description.isNotBlank()) " · ${t.description}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val day = rule.dayOfPeriod
                val dayLabel = if (day != null) " · " + stringResource(R.string.recurring_day_fmt, day) else ""
                Text(
                    text = rule.frequency.label() + dayLabel + " · " +
                        stringResource(R.string.recurring_row_next_run, DateFormatter.longDate(rule.nextRunDate)),
                    style = TextStyle(
                        fontFamily = IbmPlexMono,
                        fontSize = 11.sp,
                        color = Text3,
                        fontFeatureSettings = "tnum, lnum",
                    ),
                )
            }
        }
    }
}

@Composable
private fun RecurringFrequency.label(): String = when (this) {
    RecurringFrequency.DAILY -> stringResource(R.string.recurring_freq_daily)
    RecurringFrequency.WEEKLY -> stringResource(R.string.recurring_freq_weekly)
    RecurringFrequency.MONTHLY -> stringResource(R.string.recurring_freq_monthly)
    RecurringFrequency.YEARLY -> stringResource(R.string.recurring_freq_yearly)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecurringFormSheet(
    editing: RecurringRule?,
    onSave: (RecurringRule) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val accounts by app.accountRepository.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val cards by app.cardRepository.observeActive().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by app.categoryRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val paymentApps by app.paymentAppRepository.observeVisible().collectAsStateWithLifecycle(initialValue = emptyList())

    val existingTemplate = remember(editing) {
        editing?.transactionTemplate?.let { RecurringTemplateCodec.decode(it) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    // Template fields
    var amountText by remember(editing) { mutableStateOf(existingTemplate?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }.orEmpty()) }
    var txnType by remember(editing) { mutableStateOf(existingTemplate?.type ?: TransactionType.EXPENSE) }
    var description by remember(editing) { mutableStateOf(existingTemplate?.description.orEmpty()) }
    var sourceId by remember(editing) { mutableStateOf(existingTemplate?.sourceId) }
    var sourceType by remember(editing) { mutableStateOf(existingTemplate?.sourceType ?: SourceKind.ACCOUNT) }
    var categoryId by remember(editing) { mutableStateOf(existingTemplate?.categoryId) }
    var paymentApp by remember(editing) { mutableStateOf(existingTemplate?.paymentApp ?: "OTHER") }
    // Schedule fields
    var freq by remember(editing) { mutableStateOf(editing?.frequency ?: RecurringFrequency.MONTHLY) }
    var dayText by remember(editing) { mutableStateOf((editing?.dayOfPeriod ?: 1).toString()) }
    var autoConfirm by remember(editing) { mutableStateOf(editing?.autoConfirm ?: false) }

    val freqOptions = listOf(
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.DAILY, stringResource(R.string.recurring_freq_daily)),
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.WEEKLY, stringResource(R.string.recurring_freq_weekly)),
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.MONTHLY, stringResource(R.string.recurring_freq_monthly)),
        com.subramanya.artha.ui.common.PillOption(RecurringFrequency.YEARLY, stringResource(R.string.recurring_freq_yearly)),
    )

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
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            com.subramanya.artha.ui.common.SheetTitle(
                title = stringResource(
                    if (editing == null) R.string.recurring_form_add_title else R.string.recurring_form_edit_title,
                ),
                sub = stringResource(R.string.recurring_form_hint),
            )

            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_name_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Rent",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            // --- transaction template fields ---
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_amount_label)) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = "20000",
                    suffix = "₹",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_type_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = txnType,
                    options = listOf(
                        com.subramanya.artha.ui.common.PillOption(TransactionType.EXPENSE, stringResource(R.string.txn_type_expense)),
                        com.subramanya.artha.ui.common.PillOption(TransactionType.INCOME, stringResource(R.string.txn_type_income)),
                        com.subramanya.artha.ui.common.PillOption(TransactionType.TRANSFER, stringResource(R.string.txn_type_transfer)),
                    ),
                    onChange = { txnType = it },
                )
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_source_label)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acct ->
                        FilterChip(
                            selected = sourceId == acct.id && sourceType == SourceKind.ACCOUNT,
                            onClick = { sourceId = acct.id; sourceType = SourceKind.ACCOUNT },
                            label = { Text(acct.name, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                    cards.forEach { card ->
                        FilterChip(
                            selected = sourceId == card.id && sourceType == SourceKind.CARD,
                            onClick = { sourceId = card.id; sourceType = SourceKind.CARD },
                            label = { Text(card.name, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_description_label), optional = true) {
                com.subramanya.artha.ui.common.ArthaTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Rent — Bangalore flat",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
            val expenseCategories = remember(categories) { categories.filter { it.type == com.subramanya.artha.data.entity.enums.CategoryType.EXPENSE && it.parentId == null } }
            val incomeCategories = remember(categories) { categories.filter { it.type == com.subramanya.artha.data.entity.enums.CategoryType.INCOME && it.parentId == null } }
            val relevantCats = if (txnType == TransactionType.INCOME) incomeCategories else expenseCategories
            if (relevantCats.isNotEmpty() && txnType != TransactionType.TRANSFER) {
                com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_category_label), optional = true) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        relevantCats.forEach { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = if (categoryId == cat.id) null else cat.id },
                                label = { Text(cat.name, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.txn_payment_app_label), optional = true) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    paymentApps.forEach { app ->
                        FilterChip(
                            selected = paymentApp == app.id,
                            onClick = { paymentApp = app.id },
                            label = { Text(app.label, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }
            }
            com.subramanya.artha.ui.common.FieldRow(label = stringResource(R.string.recurring_form_frequency_label)) {
                com.subramanya.artha.ui.common.PillRadio(
                    value = freq,
                    options = freqOptions,
                    onChange = { freq = it },
                )
            }
            if (freq == RecurringFrequency.MONTHLY || freq == RecurringFrequency.WEEKLY) {
                com.subramanya.artha.ui.common.FieldRow(
                    label = if (freq == RecurringFrequency.MONTHLY)
                        stringResource(R.string.recurring_form_day_of_month)
                    else
                        stringResource(R.string.recurring_form_day_of_week),
                ) {
                    com.subramanya.artha.ui.common.ArthaTextField(
                        value = dayText,
                        onValueChange = { v -> dayText = v.filter { it.isDigit() }.take(2) },
                        placeholder = "1",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.subramanya.artha.ui.theme.Surface2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.recurring_form_auto_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.subramanya.artha.ui.theme.Text1,
                    )
                    Text(
                        text = "Fire and forget · for fixed amounts like rent",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.subramanya.artha.ui.theme.Text3,
                    )
                }
                Switch(checked = autoConfirm, onCheckedChange = { autoConfirm = it })
            }

            Spacer(Modifier.height(28.dp))
            val parsedAmount = amountText.toDoubleOrNull()
            com.subramanya.artha.ui.common.SavePrimaryButton(
                label = stringResource(R.string.common_save),
                enabled = name.isNotBlank() && parsedAmount != null && parsedAmount > 0.0 && sourceId != null,
                onClick = {
                    val now = System.currentTimeMillis()
                    val day = dayText.toIntOrNull()
                    val encodedTemplate = RecurringTemplateCodec.encode(
                        RecurringTemplate(
                            amount = parsedAmount!!,
                            type = txnType,
                            description = description.trim(),
                            sourceType = sourceType,
                            sourceId = sourceId,
                            destinationType = null,
                            destinationId = null,
                            categoryId = categoryId,
                            paymentApp = paymentApp,
                            notes = null,
                        ),
                    )
                    onSave(
                        RecurringRule(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            transactionTemplate = encodedTemplate,
                            frequency = freq,
                            dayOfPeriod = day,
                            nextRunDate = editing?.nextRunDate ?: now,
                            lastRunDate = editing?.lastRunDate,
                            autoConfirm = autoConfirm,
                            isActive = editing?.isActive ?: true,
                            createdAt = editing?.createdAt ?: now,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
