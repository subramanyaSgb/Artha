package com.subramanya.artha.ui.sms

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.domain.model.Account
import com.subramanya.artha.domain.model.Category
import com.subramanya.artha.domain.model.PendingSms
import com.subramanya.artha.ui.common.InlineTopBar
import com.subramanya.artha.ui.theme.EyebrowStyle
import com.subramanya.artha.ui.theme.Expense
import com.subramanya.artha.ui.theme.Income
import com.subramanya.artha.ui.theme.Teal500
import com.subramanya.artha.ui.theme.Text2
import com.subramanya.artha.ui.theme.Text3
import com.subramanya.artha.utils.DateFormatter

@Composable
fun PendingSmsScreen(
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: PendingSmsViewModel = viewModel(
        factory = PendingSmsViewModelFactory(
            pendingSmsRepository = app.pendingSmsRepository,
            transactionRepository = app.transactionRepository,
            accountRepository = app.accountRepository,
            categoryRepository = app.categoryRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            InlineTopBar(
                title = stringResource(R.string.pending_sms_title),
                onBack = onBack,
                trailing = {
                    if (state.items.isNotEmpty()) {
                        TextButton(onClick = vm::dismissAll) {
                            Text(stringResource(R.string.pending_sms_clear_all), color = Text3)
                        }
                    }
                },
            )

            when {
                state.loading -> com.subramanya.artha.ui.common.LoadingPlaceholder(Modifier.fillMaxSize())
                state.items.isEmpty() -> EmptyState()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id }) { pending ->
                        PendingSmsCard(
                            pending = pending,
                            accounts = state.accounts,
                            categories = state.categories,
                            onConfirm = { accountId, categoryId, amount, description ->
                                vm.confirm(pending, accountId, categoryId, amount, description, onSaved = onOpenTransaction)
                            },
                            onDismiss = { vm.dismiss(pending.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Sms, contentDescription = null, tint = Text3, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.pending_sms_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pending_sms_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = Text3,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PendingSmsCard(
    pending: PendingSms,
    accounts: List<Account>,
    categories: List<Category>,
    onConfirm: (accountId: String, categoryId: String?, amount: Double, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Local state so edits are smooth and per-card (see compose-textfield-local-state).
    var amountText by remember(pending.id) {
        mutableStateOf(pending.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }.orEmpty())
    }
    var description by remember(pending.id) { mutableStateOf(pending.merchant.orEmpty()) }
    var accountId by remember(pending.id) { mutableStateOf(pending.matchedAccountId) }
    var categoryId by remember(pending.id) { mutableStateOf(pending.suggestedCategoryId) }
    var showRaw by remember(pending.id) { mutableStateOf(false) }

    val accent = if (pending.isDebit) Expense else Income
    val amount = amountText.replace(",", "").toDoubleOrNull()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: sender + direction chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pending.sender,
                    style = EyebrowStyle,
                    color = Text3,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (pending.isDebit) R.string.pending_sms_debit else R.string.pending_sms_credit,
                    ).uppercase(),
                    style = EyebrowStyle,
                    color = accent,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { new ->
                    if (new.isEmpty() || new.matches(Regex("""^\d{0,10}(\.\d{0,2})?$"""))) amountText = new
                },
                label = { Text(stringResource(R.string.pending_sms_amount)) },
                leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium, color = accent) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedLabelColor = accent),
            )
            Spacer(Modifier.height(10.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.pending_sms_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal500, focusedLabelColor = Teal500),
            )
            Spacer(Modifier.height(10.dp))

            // Account picker (required)
            PickerRow(
                icon = Icons.Filled.AccountBalance,
                label = accounts.firstOrNull { it.id == accountId }?.name
                    ?: stringResource(R.string.pending_sms_pick_account),
                highlighted = accountId != null,
                options = accounts.map { it.id to it.name },
                selectedId = accountId,
                onSelect = { accountId = it },
            )
            Spacer(Modifier.height(8.dp))

            // Category picker (optional)
            PickerRow(
                icon = Icons.Filled.Category,
                label = categories.firstOrNull { it.id == categoryId }?.name
                    ?: stringResource(R.string.pending_sms_pick_category),
                highlighted = categoryId != null,
                options = categories.map { it.id to it.name },
                selectedId = categoryId,
                onSelect = { categoryId = it },
            )

            // Raw SMS (collapsible)
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (showRaw) stringResource(R.string.pending_sms_hide_original)
                else stringResource(R.string.pending_sms_show_original),
                style = MaterialTheme.typography.labelSmall,
                color = Teal500,
                modifier = Modifier.clickable { showRaw = !showRaw },
            )
            if (showRaw) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = pending.rawBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(10.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.pending_sms_dismiss), color = Text3)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (amount != null && accountId != null) onConfirm(accountId!!, categoryId, amount, description) },
                    enabled = amount != null && amount > 0 && accountId != null,
                    modifier = Modifier.weight(1.4f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                ) {
                    Text(
                        stringResource(R.string.pending_sms_confirm),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }

            // Date footnote
            Spacer(Modifier.height(6.dp))
            Text(
                text = DateFormatter.longDate(pending.effectiveDate),
                style = MaterialTheme.typography.labelSmall,
                color = Text3,
            )
        }
    }
}

@Composable
private fun PickerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (highlighted) Teal500.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp),
                )
                .clickable(enabled = options.isNotEmpty()) { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (highlighted) Teal500 else Text3, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (highlighted) MaterialTheme.colorScheme.onSurface else Text3,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Text3, modifier = Modifier.size(18.dp))
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
