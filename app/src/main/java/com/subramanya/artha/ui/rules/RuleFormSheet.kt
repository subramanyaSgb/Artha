package com.subramanya.artha.ui.rules

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.PaymentApp
import com.subramanya.artha.data.entity.enums.PersonRelation
import com.subramanya.artha.data.entity.enums.SourceKind
import com.subramanya.artha.data.entity.enums.TransactionType
import com.subramanya.artha.domain.model.TransactionRule
import com.subramanya.artha.domain.rules.AmountOp
import com.subramanya.artha.domain.rules.ConditionLogic
import com.subramanya.artha.domain.rules.RuleAction
import com.subramanya.artha.domain.rules.RuleActions
import com.subramanya.artha.domain.rules.RuleCondition
import com.subramanya.artha.domain.rules.RuleConditions
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Visual rule builder (PRD §7.20). Edits two lists — conditions + actions — with
 * variant pickers per item. Not a free-form expression editor; intentionally
 * constrained to the typed RuleCondition / RuleAction sealed hierarchies so a
 * malformed rule is impossible to construct.
 *
 * System rules can be edited (per PRD), so we don't disable any field; deletion
 * is blocked elsewhere (RulesScreen warns before deleting a system rule).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleFormSheet(
    editing: TransactionRule?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var logic by remember(editing) {
        mutableStateOf(editing?.conditions?.logic ?: ConditionLogic.ALL)
    }
    val conditions: SnapshotConditionList = remember(editing) {
        editing?.conditions?.items.orEmpty().toMutableStateList()
    }
    val actions: SnapshotActionList = remember(editing) {
        editing?.actions?.items.orEmpty().toMutableStateList()
    }
    var priorityText by remember(editing) {
        mutableStateOf((editing?.priority ?: DEFAULT_USER_PRIORITY).toString())
    }
    var showErrors by remember { mutableStateOf(false) }

    val parsedPriority = priorityText.toIntOrNull()
    val isValid = name.isNotBlank() && parsedPriority != null && actions.isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(
                    if (editing == null) R.string.rules_form_add_title
                    else R.string.rules_form_edit_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.rules_form_name_label)) },
                placeholder = { Text(stringResource(R.string.rules_form_name_placeholder)) },
                isError = showErrors && name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = priorityText,
                onValueChange = { v -> priorityText = v.filter { it.isDigit() } },
                singleLine = true,
                label = { Text(stringResource(R.string.rules_form_priority_label)) },
                supportingText = { Text(stringResource(R.string.rules_form_priority_hint)) },
                isError = showErrors && parsedPriority == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.rules_form_section_when), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = logic == ConditionLogic.ALL,
                    onClick = { logic = ConditionLogic.ALL },
                    label = { Text(stringResource(R.string.rules_form_logic_all)) },
                )
                FilterChip(
                    selected = logic == ConditionLogic.ANY,
                    onClick = { logic = ConditionLogic.ANY },
                    label = { Text(stringResource(R.string.rules_form_logic_any)) },
                )
            }

            Spacer(Modifier.height(8.dp))
            conditions.forEachIndexed { index, condition ->
                ConditionRow(
                    condition = condition,
                    onChange = { conditions[index] = it },
                    onRemove = { conditions.removeAt(index) },
                )
                Spacer(Modifier.height(4.dp))
            }

            AddConditionButton(onAdd = { conditions.add(it) })

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.rules_form_section_then), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            actions.forEachIndexed { index, action ->
                ActionRow(
                    action = action,
                    onChange = { actions[index] = it },
                    onRemove = { actions.removeAt(index) },
                )
                Spacer(Modifier.height(4.dp))
            }
            if (showErrors && actions.isEmpty()) {
                Text(
                    text = stringResource(R.string.rules_form_validation_no_actions),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            AddActionButton(onAdd = { actions.add(it) })

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!isValid) {
                        showErrors = true
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val rule = TransactionRule(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        conditions = RuleConditions(logic = logic, items = conditions.toList()),
                        actions = RuleActions(items = actions.toList()),
                        priority = parsedPriority ?: DEFAULT_USER_PRIORITY,
                        isActive = editing?.isActive ?: true,
                        isSystem = editing?.isSystem ?: false,
                        createdAt = editing?.createdAt ?: now,
                    )
                    scope.launch {
                        app.transactionRuleRepository.upsert(rule)
                        onDismiss()
                    }
                },
                enabled = !showErrors || isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.rules_form_save)) }
        }
    }
}

private typealias SnapshotConditionList = androidx.compose.runtime.snapshots.SnapshotStateList<RuleCondition>
private typealias SnapshotActionList = androidx.compose.runtime.snapshots.SnapshotStateList<RuleAction>

// ---------------- conditions ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionRow(
    condition: RuleCondition,
    onChange: (RuleCondition) -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
            Text(condition.summary(), style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            // Per-condition mini-editor based on variant — simple inline fields.
            ConditionEditor(condition = condition, onChange = onChange)
        },
        trailingContent = {
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = null) }
        },
    )
}

@Composable
private fun ConditionEditor(condition: RuleCondition, onChange: (RuleCondition) -> Unit) {
    when (condition) {
        is RuleCondition.DescriptionContains -> {
            OutlinedTextField(
                value = condition.text,
                onValueChange = { onChange(condition.copy(text = it)) },
                singleLine = true,
                label = { Text(stringResource(R.string.rules_cond_desc_contains_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is RuleCondition.AmountCompare -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountOp.entries.forEach { op ->
                    FilterChip(
                        selected = condition.op == op,
                        onClick = { onChange(condition.copy(op = op)) },
                        label = { Text(op.label()) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = condition.value.toPlainStringOrEmpty(),
                onValueChange = { v ->
                    val parsed = v.toDoubleOrNull() ?: 0.0
                    onChange(condition.copy(value = parsed))
                },
                singleLine = true,
                label = { Text(stringResource(R.string.rules_cond_amount_label)) },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is RuleCondition.TypeIs -> EnumPicker(
            label = stringResource(R.string.rules_cond_type_label),
            current = condition.type,
            options = TransactionType.entries,
            onPick = { onChange(condition.copy(type = it)) },
            labelFor = { it.name },
        )
        is RuleCondition.PaymentAppIs -> EnumPicker(
            label = stringResource(R.string.rules_cond_payment_app_label),
            current = condition.app,
            options = PaymentApp.entries,
            onPick = { onChange(condition.copy(app = it)) },
            labelFor = { it.name },
        )
        is RuleCondition.HasPersonRelation -> EnumPicker(
            label = stringResource(R.string.rules_cond_person_relation_label),
            current = condition.relation,
            options = PersonRelation.entries,
            onPick = { onChange(condition.copy(relation = it)) },
            labelFor = { it.name },
        )
        is RuleCondition.SourceIs -> SourceKindEditor(
            label = stringResource(R.string.rules_cond_source_label),
            kind = condition.kind,
            id = condition.id,
            onChange = { kind, id -> onChange(condition.copy(kind = kind, id = id)) },
        )
        is RuleCondition.DestinationIs -> SourceKindEditor(
            label = stringResource(R.string.rules_cond_destination_label),
            kind = condition.kind,
            id = condition.id,
            onChange = { kind, id -> onChange(condition.copy(kind = kind, id = id)) },
        )
        is RuleCondition.TimeOfDayBetween -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = (condition.fromMinuteOfDay / 60).toString(),
                onValueChange = { v ->
                    val h = v.toIntOrNull()?.coerceIn(0, 23) ?: 0
                    onChange(condition.copy(fromMinuteOfDay = h * 60))
                },
                label = { Text(stringResource(R.string.rules_cond_time_from)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = (condition.toMinuteOfDay / 60).toString(),
                onValueChange = { v ->
                    val h = v.toIntOrNull()?.coerceIn(0, 23) ?: 23
                    onChange(condition.copy(toMinuteOfDay = h * 60))
                },
                label = { Text(stringResource(R.string.rules_cond_time_to)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
private fun SourceKindEditor(
    label: String,
    kind: SourceKind,
    id: String?,
    onChange: (SourceKind, String?) -> Unit,
) {
    Column {
        EnumPicker(
            label = label,
            current = kind,
            options = SourceKind.entries,
            onPick = { onChange(it, id) },
            labelFor = { it.name },
        )
        OutlinedTextField(
            value = id.orEmpty(),
            onValueChange = { onChange(kind, it.takeIf { v -> v.isNotBlank() }) },
            singleLine = true,
            label = { Text(stringResource(R.string.rules_cond_source_id_label)) },
            placeholder = { Text(stringResource(R.string.rules_cond_source_id_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun <T> EnumPicker(
    label: String,
    current: T,
    options: List<T>,
    onPick: (T) -> Unit,
    labelFor: (T) -> String,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text("$label: ${labelFor(current)}") },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelFor(option)) },
                    onClick = { onPick(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun AddConditionButton(onAdd: (RuleCondition) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.rules_form_add_condition))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_desc)) },
                onClick = { onAdd(RuleCondition.DescriptionContains(text = "")); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_amount)) },
                onClick = { onAdd(RuleCondition.AmountCompare(op = AmountOp.GTE, value = 0.0)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_type)) },
                onClick = { onAdd(RuleCondition.TypeIs(type = TransactionType.EXPENSE)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_payment_app)) },
                onClick = { onAdd(RuleCondition.PaymentAppIs(app = PaymentApp.GPAY)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_person_relation)) },
                onClick = { onAdd(RuleCondition.HasPersonRelation(relation = PersonRelation.SPOUSE)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_source)) },
                onClick = { onAdd(RuleCondition.SourceIs(kind = SourceKind.ACCOUNT, id = null)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_destination)) },
                onClick = { onAdd(RuleCondition.DestinationIs(kind = SourceKind.CARD, id = null)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_cond_kind_time)) },
                onClick = { onAdd(RuleCondition.TimeOfDayBetween(fromMinuteOfDay = 9 * 60, toMinuteOfDay = 18 * 60)); expanded = false },
            )
        }
    }
}

// ---------------- actions ----------------

@Composable
private fun ActionRow(
    action: RuleAction,
    onChange: (RuleAction) -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
            Text(action.summary(), style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            ActionEditor(action = action, onChange = onChange)
        },
        trailingContent = {
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = null) }
        },
    )
}

@Composable
private fun ActionEditor(action: RuleAction, onChange: (RuleAction) -> Unit) {
    when (action) {
        is RuleAction.SetType -> EnumPicker(
            label = stringResource(R.string.rules_action_set_type_label),
            current = action.type,
            options = TransactionType.entries,
            onPick = { onChange(action.copy(type = it)) },
            labelFor = { it.name },
        )
        is RuleAction.SetCategory -> Column {
            OutlinedTextField(
                value = action.categoryId,
                onValueChange = { onChange(action.copy(categoryId = it)) },
                singleLine = true,
                label = { Text(stringResource(R.string.rules_action_category_id_label)) },
                placeholder = { Text("cat_food_drink") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = action.subCategoryId.orEmpty(),
                onValueChange = {
                    onChange(action.copy(subCategoryId = it.takeIf { v -> v.isNotBlank() }))
                },
                singleLine = true,
                label = { Text(stringResource(R.string.rules_action_subcategory_id_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is RuleAction.SetTaxSection -> OutlinedTextField(
            value = action.section,
            onValueChange = { onChange(action.copy(section = it.uppercase())) },
            singleLine = true,
            label = { Text(stringResource(R.string.rules_action_tax_section_label)) },
            placeholder = { Text("80C") },
            modifier = Modifier.fillMaxWidth(),
        )
        is RuleAction.AddTag -> OutlinedTextField(
            value = action.tagId,
            onValueChange = { onChange(action.copy(tagId = it)) },
            singleLine = true,
            label = { Text(stringResource(R.string.rules_action_tag_id_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        is RuleAction.AddPerson -> OutlinedTextField(
            value = action.personId,
            onValueChange = { onChange(action.copy(personId = it)) },
            singleLine = true,
            label = { Text(stringResource(R.string.rules_action_person_id_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        RuleAction.ExcludeFromExpenseTotal, RuleAction.PromptSpouse -> Unit
    }
}

@Composable
private fun AddActionButton(onAdd: (RuleAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.rules_form_add_action))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_set_type)) },
                onClick = { onAdd(RuleAction.SetType(TransactionType.EXPENSE)); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_set_category)) },
                onClick = { onAdd(RuleAction.SetCategory(categoryId = "")); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_set_tax)) },
                onClick = { onAdd(RuleAction.SetTaxSection(section = "")); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_add_tag)) },
                onClick = { onAdd(RuleAction.AddTag(tagId = "")); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_add_person)) },
                onClick = { onAdd(RuleAction.AddPerson(personId = "")); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_exclude_expense)) },
                onClick = { onAdd(RuleAction.ExcludeFromExpenseTotal); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rules_action_kind_prompt_spouse)) },
                onClick = { onAdd(RuleAction.PromptSpouse); expanded = false },
            )
        }
    }
}

// ---------------- summaries ----------------

@Composable
private fun RuleCondition.summary(): String = when (this) {
    is RuleCondition.DescriptionContains -> stringResource(R.string.rules_cond_summary_desc, text)
    is RuleCondition.AmountCompare -> stringResource(R.string.rules_cond_summary_amount, op.label(), value)
    is RuleCondition.TypeIs -> stringResource(R.string.rules_cond_summary_type, type.name)
    is RuleCondition.PaymentAppIs -> stringResource(R.string.rules_cond_summary_payment_app, app.name)
    is RuleCondition.HasPersonRelation -> stringResource(R.string.rules_cond_summary_person, relation.name)
    is RuleCondition.SourceIs -> stringResource(R.string.rules_cond_summary_source, kind.name, id ?: "*")
    is RuleCondition.DestinationIs -> stringResource(R.string.rules_cond_summary_destination, kind.name, id ?: "*")
    is RuleCondition.TimeOfDayBetween -> stringResource(R.string.rules_cond_summary_time, fromMinuteOfDay / 60, toMinuteOfDay / 60)
}

@Composable
private fun RuleAction.summary(): String = when (this) {
    is RuleAction.SetType -> stringResource(R.string.rules_action_summary_set_type, type.name)
    is RuleAction.SetCategory -> stringResource(R.string.rules_action_summary_set_category, categoryId)
    is RuleAction.SetTaxSection -> stringResource(R.string.rules_action_summary_set_tax, section)
    is RuleAction.AddTag -> stringResource(R.string.rules_action_summary_add_tag, tagId)
    is RuleAction.AddPerson -> stringResource(R.string.rules_action_summary_add_person, personId)
    RuleAction.ExcludeFromExpenseTotal -> stringResource(R.string.rules_action_summary_exclude)
    RuleAction.PromptSpouse -> stringResource(R.string.rules_action_summary_spouse)
}

@Composable
private fun AmountOp.label(): String = when (this) {
    AmountOp.EQ -> "="
    AmountOp.GT -> ">"
    AmountOp.LT -> "<"
    AmountOp.GTE -> "≥"
    AmountOp.LTE -> "≤"
}

private fun Double.toPlainStringOrEmpty(): String =
    if (this == 0.0) "" else if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

/** User-created rules sort below the seeded ones (priorities 10..90). */
private const val DEFAULT_USER_PRIORITY: Int = 100
