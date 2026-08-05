package com.subramanya.artha.ui.insurance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.ui.theme.ArthaAmountStyles
import com.subramanya.artha.utils.DateFormatter
import com.subramanya.artha.utils.IndianNumberFormat
import com.subramanya.artha.utils.PolicyDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceDetailScreen(
    insuranceId: String,
    onBack: () -> Unit,
    onOpenInvestment: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ArthaApplication
    val vm: InsuranceDetailViewModel = viewModel(
        factory = InsuranceDetailViewModelFactory(
            insuranceId = insuranceId,
            insuranceRepository = app.insuranceRepository,
            investmentRepository = app.investmentRepository,
        ),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var editing: Insurance? by remember { mutableStateOf(null) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val ins = state.insurance
            com.subramanya.artha.ui.common.InlineTopBar(
                title = ins?.name.orEmpty(),
                onBack = onBack,
                trailing = {
                    if (ins != null) {
                        IconButton(onClick = { editing = ins }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.account_detail_action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (ins.isArchived) {
                            IconButton(onClick = { vm.restore(onRestored = onBack) }) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = stringResource(R.string.account_detail_action_restore),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            IconButton(onClick = { vm.archive(onArchived = onBack) }) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = stringResource(R.string.account_detail_action_archive),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = vm::requestDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.account_action_delete),
                                tint = com.subramanya.artha.ui.theme.Danger,
                            )
                        }
                    }
                },
            )
            if (ins == null) {
                com.subramanya.artha.ui.common.LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    HeroBlock(insurance = ins)
                    MetaBlock(insurance = ins)

                    // Rich sections from the uploaded-policy blob. Each is null/empty for
                    // a manually-entered policy, so nothing below renders in that case.
                    state.details?.let { PolicyDetailSections(details = it) }

                    if (ins.policyDocUri != null) {
                        DocumentsSection(policyDocUri = ins.policyDocUri)
                    }

                    state.linkedInvestment?.let { inv ->
                        Spacer(Modifier.height(8.dp))
                        LinkedInvestmentCard(name = inv.name, onClick = { onOpenInvestment(inv.id) })
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    val editingNow = editing
    if (editingNow != null) {
        InsuranceFormSheet(editing = editingNow, onDismiss = { editing = null })
    }

    if (state.showDeleteConfirm) {
        com.subramanya.artha.ui.common.ArthaAlertDialog(
            onDismissRequest = vm::dismissDeleteConfirm,
            title = stringResource(R.string.insurance_delete_confirm_title),
            text = stringResource(R.string.insurance_delete_confirm_body),
            confirmLabel = stringResource(R.string.insurance_delete_confirm_yes),
            confirmDestructive = true,
            onConfirm = { vm.confirmDelete(onDeleted = onBack) },
            cancelLabel = stringResource(R.string.common_cancel),
            onCancel = vm::dismissDeleteConfirm,
        )
    }
}

// ---------------- pieces ----------------

@Composable
private fun HeroBlock(insurance: Insurance) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(insurance.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = insuranceTypeDisplayName(insurance.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = insurance.provider,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.insurance_detail_sum_assured_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = IndianNumberFormat.format(insurance.sumAssured),
                style = ArthaAmountStyles.display,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    text = stringResource(
                        R.string.insurance_detail_premium_fmt,
                        IndianNumberFormat.format(insurance.premiumAmount),
                        insurance.premiumFrequency.displayName(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MetaBlock(insurance: Insurance) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        insurance.planName?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_plan_name), it)
        }
        insurance.policyNumber?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_policy_number), it)
        }
        insurance.uin?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_uin), it)
        }
        insurance.lifeAssured?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_life_assured), it)
        }
        insurance.policyTerm?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_policy_term), it)
        }
        MetaRow(stringResource(R.string.insurance_detail_start), DateFormatter.longDate(insurance.startDate))
        insurance.nextPremiumDate?.let { nextDue ->
            // Countdown so the user sees "due in N days" (or "overdue") at a glance.
            val days = ((nextDue - System.currentTimeMillis()) / 86_400_000L).toInt()
            val suffix = if (days >= 0) {
                androidx.compose.ui.res.pluralStringResource(R.plurals.insurance_detail_due_in, days, days)
            } else {
                stringResource(R.string.insurance_detail_overdue)
            }
            MetaRow(
                stringResource(R.string.insurance_detail_next_due),
                "${DateFormatter.longDate(nextDue)} · $suffix",
            )
        }
        insurance.endDate?.let {
            MetaRow(stringResource(R.string.insurance_detail_end), DateFormatter.longDate(it))
        }
        insurance.nominee?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_nominee), it)
        }
        insurance.taxSection?.takeIf { it.isNotBlank() }?.let {
            MetaRow(stringResource(R.string.insurance_detail_tax_section), it)
        }
        insurance.agentContact?.takeIf { it.isNotBlank() }?.let { contact ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.insurance_detail_agent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact, style = MaterialTheme.typography.bodyMedium)
                    val phoneLike = contact.filter { it.isDigit() || it == '+' }
                    if (phoneLike.length >= 7) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneLike")
                            }
                            runCatching { context.startActivity(intent) }
                        }) {
                            Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.insurance_detail_call_agent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LinkedInvestmentCard(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insurance_detail_linked_investment_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ---------------- rich policy sections ----------------

@Composable
private fun PolicyDetailSections(details: PolicyDetails) {
    if (details.members.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.insurance_detail_members_title)) {
            details.members.forEach { m ->
                val subtitle = listOfNotNull(m.relation, m.age).takeIf { it.isNotEmpty() }?.let {
                    if (m.relation != null && m.age != null) {
                        stringResource(R.string.insurance_detail_member_relation_age, m.relation, m.age)
                    } else {
                        it.joinToString(" · ")
                    }
                }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(m.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (details.coverage.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.insurance_detail_coverage_title)) {
            details.coverage.forEach { MetaRow(it.label, it.value) }
        }
    }

    if (details.riders.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.insurance_detail_riders_title)) {
            details.riders.forEach { r ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(r.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        r.premium?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    r.note?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (details.exclusions.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.insurance_detail_exclusions_title)) {
            details.exclusions.forEach { ex ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(ex, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    details.premiumBreakdown?.let { pb ->
        SectionCard(title = stringResource(R.string.insurance_detail_premium_breakdown_title)) {
            pb.base?.let { MetaRow(stringResource(R.string.insurance_detail_premium_base), it) }
            pb.riders?.let { MetaRow(stringResource(R.string.insurance_detail_premium_riders), it) }
            pb.gst?.let { MetaRow(stringResource(R.string.insurance_detail_premium_gst), it) }
            pb.total?.let { MetaRow(stringResource(R.string.insurance_detail_premium_total), it) }
        }
    }

    details.contacts?.let { c ->
        SectionCard(title = stringResource(R.string.insurance_detail_contacts_title)) {
            c.helpline?.let { DialableRow(stringResource(R.string.insurance_detail_helpline), it) }
            c.claimsEmail?.let { MetaRow(stringResource(R.string.insurance_detail_claims_email), it) }
            c.branch?.let { MetaRow(stringResource(R.string.insurance_detail_branch), it) }
            c.tpa?.let { MetaRow(stringResource(R.string.insurance_detail_tpa), it) }
        }
    }
}

/** Section wrapper matching the linked-investment card styling. */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** Label + value with a dial button — reuses the agent-contact pattern for phone-like values. */
@Composable
private fun DialableRow(label: String, value: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            val phoneLike = value.filter { it.isDigit() || it == '+' }
            if (phoneLike.length >= 7) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneLike") }
                    runCatching { context.startActivity(intent) }
                }) {
                    Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.insurance_detail_call_helpline))
                }
            }
        }
    }
}

/**
 * "View policy document" — the uploaded doc is persisted through ReceiptStore, which
 * re-encodes it to a JPEG in filesDir/receipts. So it's an image, not a live PDF;
 * we show it fullscreen via the same viewer the receipt flow uses (no FileProvider
 * for filesDir exists, and ACTION_VIEW on a JPEG-of-a-PDF would be misleading).
 */
@Composable
private fun DocumentsSection(policyDocUri: String) {
    val context = LocalContext.current
    var fullscreen by remember { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.insurance_detail_documents_title)) {
        TextButton(onClick = { fullscreen = true }) {
            Icon(Icons.Filled.Description, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.insurance_detail_view_policy))
        }
    }

    if (fullscreen) {
        val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
            initialValue = null,
            policyDocUri,
        ) {
            value = com.subramanya.artha.utils.ReceiptStore.loadBitmap(context, policyDocUri)
        }
        androidx.compose.ui.window.Dialog(onDismissRequest = { fullscreen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { fullscreen = false },
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.insurance_detail_view_policy),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumFrequency.displayName(): String = when (this) {
    PremiumFrequency.MONTHLY -> stringResource(R.string.premium_frequency_monthly)
    PremiumFrequency.QUARTERLY -> stringResource(R.string.premium_frequency_quarterly)
    PremiumFrequency.HALF_YEARLY -> stringResource(R.string.premium_frequency_half_yearly)
    PremiumFrequency.YEARLY -> stringResource(R.string.premium_frequency_yearly)
    PremiumFrequency.SINGLE -> stringResource(R.string.premium_frequency_single)
}
