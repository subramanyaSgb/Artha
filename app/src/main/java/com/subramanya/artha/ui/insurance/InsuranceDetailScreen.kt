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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.R
import com.subramanya.artha.data.entity.enums.PremiumFrequency
import com.subramanya.artha.domain.model.Insurance
import com.subramanya.artha.ui.theme.Manrope
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
        color = CertTokens.pageBg,
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
                CertificateBody(
                    ins = ins,
                    details = state.details,
                    linkedInvestmentName = state.linkedInvestment?.name,
                    onOpenInvestment = { state.linkedInvestment?.let { onOpenInvestment(it.id) } },
                )
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

/**
 * The full 1:1 policy-certificate detail body: ornate hero + quick facts + validity
 * + the numbered "schedule" accordions, all composed from real data. Every section
 * hides when its data is empty, so a manually-entered policy (no detailsJson) still
 * renders cleanly: hero, quick facts, validity (if endDate), Policy Schedule,
 * Premium (from premiumAmount), documents (if any).
 */
@Composable
private fun CertificateBody(
    ins: Insurance,
    details: PolicyDetails?,
    linkedInvestmentName: String?,
    onOpenInvestment: () -> Unit,
) {
    val context = LocalContext.current
    val kindLabel = policyKindLabel(ins.type)

    // ---- shared derived values ----
    val startText = DateFormatter.longDate(ins.startDate)
    val endText = ins.endDate?.let { DateFormatter.longDate(it) }
    val members = details?.members ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Hero
        PolicyCertificateHero(
            insurer = ins.provider,
            statusLabel = details?.status,
            planName = ins.planName?.takeIf { it.isNotBlank() } ?: ins.name,
            policyKind = kindLabel,
            sumInsuredFormatted = IndianNumberFormat.format(ins.sumAssured),
            sumInsuredWords = null, // no words field available
            policyNumber = ins.policyNumber,
            issuedOn = startText.uppercase(),
            lifeAssured = ins.lifeAssured?.takeIf { it.isNotBlank() },
            uin = ins.uin?.takeIf { it.isNotBlank() },
        )

        // 2. Quick facts
        QuickFactsGrid(
            started = startText,
            expires = endText,
            members = members.size.takeIf { it > 0 }
                ?.let { stringResource(R.string.insurance_cert_members_count, it) },
            policyType = kindLabel,
            premium = ins.premiumAmount.takeIf { it > 0.0 }?.let { IndianNumberFormat.format(it) },
            premiumFreqSuffix = ins.premiumAmount.takeIf { it > 0.0 }
                ?.let { stringResource(R.string.insurance_cert_premium_freq_suffix, ins.premiumFrequency.freqShort()) },
            taxBenefit = ins.taxSection?.takeIf { it.isNotBlank() },
        )

        // 3. Validity (only if we know the end date)
        if (ins.endDate != null) {
            ValidityBlock(startMillis = ins.startDate, endMillis = ins.endDate, startText = startText, endText = endText.orEmpty())
        }

        // ---- schedule accordions ----
        val hasCoverage = details?.coverage?.isNotEmpty() == true
        val hasRiders = details?.riders?.isNotEmpty() == true
        val hasExclusions = details?.exclusions?.isNotEmpty() == true
        val hasContacts = details?.contacts != null || !ins.insurerHelpline.isNullOrBlank()
        val hasPremium = details?.premiumBreakdown != null || ins.premiumAmount > 0.0
        val hasNominee = !ins.nominee.isNullOrBlank()

        // Divider shows if any schedule content follows (Policy Schedule always does).
        ScheduleDivider(stringResource(R.string.insurance_cert_schedule_divider))

        // I — Policy Schedule (always: at least the commencement date exists)
        AccordionSection(
            numeral = stringResource(R.string.insurance_cert_num_i),
            title = stringResource(R.string.insurance_cert_section_policy_schedule),
            initiallyOpen = true,
        ) {
            ins.planName?.takeIf { it.isNotBlank() }
                ?.let { MetaRow(stringResource(R.string.insurance_detail_plan_name), it) }
            ins.policyNumber?.takeIf { it.isNotBlank() }
                ?.let { MetaRow(stringResource(R.string.insurance_detail_policy_number), it) }
            ins.uin?.takeIf { it.isNotBlank() }
                ?.let { MetaRow(stringResource(R.string.insurance_detail_uin), it) }
            ins.policyTerm?.takeIf { it.isNotBlank() }
                ?.let { MetaRow(stringResource(R.string.insurance_detail_policy_term), it) }
            MetaRow(stringResource(R.string.insurance_cert_commencement), startText)
            endText?.let { MetaRow(stringResource(R.string.insurance_cert_expiry), it) }
        }

        // II — Insured Members
        if (members.isNotEmpty()) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_ii),
                title = stringResource(R.string.insurance_cert_section_members),
            ) {
                members.forEachIndexed { i, m ->
                    if (i > 0) Spacer(Modifier.size(8.dp))
                    MemberRow(
                        initials = initialsOf(m.name),
                        name = m.name,
                        sub = memberSub(m),
                        avatarGradient = avatarGradientFor(i),
                        proposer = i == 0,
                    )
                }
            }
        }

        // III — Nominee
        if (hasNominee) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_iii),
                title = stringResource(R.string.insurance_cert_section_nominee),
            ) {
                MetaRow(stringResource(R.string.insurance_detail_nominee), ins.nominee.orEmpty())
            }
        }

        // IV — Coverage & Benefits
        if (hasCoverage) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_iv),
                title = stringResource(R.string.insurance_cert_section_coverage),
            ) {
                details?.coverage?.forEach { MetaRow(it.label, it.value) }
            }
        }

        // V — Premium & Payment
        if (hasPremium) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_v),
                title = stringResource(R.string.insurance_cert_section_premium),
            ) {
                val pb = details?.premiumBreakdown
                pb?.base?.let { MetaRow(stringResource(R.string.insurance_detail_premium_base), it) }
                pb?.riders?.let { MetaRow(stringResource(R.string.insurance_detail_premium_riders), it) }
                pb?.gst?.let { MetaRow(stringResource(R.string.insurance_detail_premium_gst), it) }
                val total = pb?.total ?: IndianNumberFormat.format(ins.premiumAmount)
                PremiumTotalRow(stringResource(R.string.insurance_detail_premium_total), total)
            }
        }

        // VI — Riders & Add-ons
        if (hasRiders) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_vi),
                title = stringResource(R.string.insurance_cert_section_riders),
            ) {
                details?.riders?.forEachIndexed { i, r ->
                    if (i > 0) Spacer(Modifier.size(8.dp))
                    RiderRow(name = r.name, premium = r.premium, note = r.note)
                }
            }
        }

        // VII — Waiting Periods & Exclusions
        if (hasExclusions) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_vii),
                title = stringResource(R.string.insurance_cert_section_exclusions),
            ) {
                details?.exclusions?.forEach { ExclusionRow(it) }
            }
        }

        // VIII — Insurer Contacts
        if (hasContacts) {
            AccordionSection(
                numeral = stringResource(R.string.insurance_cert_num_viii),
                title = stringResource(R.string.insurance_cert_section_contacts),
            ) {
                val c = details?.contacts
                val helpline = (c?.helpline ?: ins.insurerHelpline)?.takeIf { it.isNotBlank() }
                // Build the contact list so we can space rows uniformly (8.dp between).
                val rows = buildList<@Composable () -> Unit> {
                    helpline?.let { v ->
                        add {
                            ContactRow(
                                icon = Icons.Filled.SupportAgent,
                                label = stringResource(R.string.insurance_detail_helpline),
                                value = v,
                                trailingAction = dialAction(context, v),
                            )
                        }
                    }
                    c?.claimsEmail?.let { v ->
                        add { ContactRow(Icons.Filled.Email, stringResource(R.string.insurance_detail_claims_email), v) }
                    }
                    c?.branch?.let { v ->
                        add { ContactRow(Icons.Filled.Business, stringResource(R.string.insurance_detail_branch), v) }
                    }
                    c?.tpa?.let { v ->
                        add { ContactRow(Icons.Filled.LocalHospital, stringResource(R.string.insurance_detail_tpa), v) }
                    }
                }
                rows.forEachIndexed { i, row ->
                    if (i > 0) Spacer(Modifier.size(8.dp))
                    row()
                }
            }
        }

        // Documents (only if a doc is stored) — reuses the verbatim PolicyDocStore logic.
        if (ins.policyDocUri != null) {
            DocumentsBlock(policyDocUri = ins.policyDocUri)
        }

        // Renewal reminder (if we have a date to remind against)
        val reminderDate = ins.nextPremiumDate ?: ins.endDate
        if (reminderDate != null) {
            RenewalReminderCard(
                text = stringResource(
                    R.string.insurance_cert_reminder_body,
                    DateFormatter.longDate(reminderDate),
                ),
            )
        }

        // Linked investment (kept from the old screen)
        linkedInvestmentName?.let { name ->
            LinkedInvestmentCard(name = name, onClick = onOpenInvestment)
        }

        FooterDisclaimer(stringResource(R.string.insurance_cert_disclaimer, ins.provider))

        Spacer(Modifier.size(8.dp))
    }
}

// ---------------- validity math ----------------

@Composable
private fun ValidityBlock(startMillis: Long, endMillis: Long, startText: String, endText: String) {
    val now = System.currentTimeMillis()
    val span = (endMillis - startMillis).coerceAtLeast(1L)
    val fraction = ((now - startMillis).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val expired = now >= endMillis

    val elapsedDays = ((now - startMillis) / 86_400_000L).coerceAtLeast(0L).toInt()
    val remainingDays = (((endMillis - now) / 86_400_000L)).coerceAtLeast(0L).toInt()

    ValidityCard(
        rangeText = stringResource(R.string.insurance_cert_range_dash, startText, endText).uppercase(),
        elapsedFraction = fraction,
        elapsedLabel = stringResource(
            R.string.insurance_cert_elapsed,
            stringResource(R.string.insurance_cert_days, elapsedDays),
        ),
        remainingLabel = if (expired) {
            stringResource(R.string.insurance_cert_expired)
        } else {
            stringResource(
                R.string.insurance_cert_remaining,
                stringResource(R.string.insurance_cert_days, remainingDays),
            )
        },
        validStamp = if (expired) {
            stringResource(R.string.insurance_cert_expired)
        } else {
            stringResource(R.string.insurance_cert_valid)
        },
    )
}

// ---------------- documents (reuses PolicyDocStore verbatim-view logic) ----------------

@Composable
private fun DocumentsBlock(policyDocUri: String) {
    val context = LocalContext.current
    val isPdf = policyDocUri.substringAfterLast('.').equals("pdf", ignoreCase = true)
    var fullscreen by remember { mutableStateOf(false) }
    val noViewerMsg = stringResource(R.string.insurance_detail_no_pdf_viewer)
    val label = stringResource(
        if (isPdf) R.string.insurance_detail_view_policy_pdf else R.string.insurance_detail_view_policy,
    )

    DocumentsRow(label = label, onViewPdf = {
        if (isPdf) {
            val intent = com.subramanya.artha.utils.PolicyDocStore.viewIntent(context, policyDocUri)
            if (intent == null || runCatching { context.startActivity(intent) }.isFailure) {
                android.widget.Toast.makeText(context, noViewerMsg, android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            fullscreen = true
        }
    })

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

// ---------------- linked investment (kept from old screen) ----------------

@Composable
private fun LinkedInvestmentCard(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CertTokens.cardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(CertTokens.gold.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Savings, contentDescription = null, tint = CertTokens.gold, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insurance_detail_linked_investment_title).uppercase(),
                    style = CertTokens.goldMicroLabel,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = name,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = Manrope,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CertTokens.textPrimary,
                    ),
                )
            }
        }
    }
}

// ---------------- small helpers ----------------

/** A "Call" trailing action for a phone-like contact value; null if not dialable. */
@Composable
private fun dialAction(context: android.content.Context, value: String): (@Composable () -> Unit)? {
    val phoneLike = value.filter { it.isDigit() || it == '+' }
    if (phoneLike.length < 7) return null
    val callLabel = stringResource(R.string.insurance_cert_call)
    return {
        Text(
            text = callLabel,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneLike") }
                runCatching { context.startActivity(intent) }
            },
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = Manrope,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CertTokens.successGreen,
            ),
        )
    }
}

/** Member subtitle: "relation · age" / "relation" / "age" / null. */
private fun memberSub(m: PolicyDetails.Member): String? =
    listOfNotNull(m.relation, m.age).takeIf { it.isNotEmpty() }?.joinToString(" · ")

/** Insurance type → human "… Insurance Policy" label for the hero / quick facts. */
@Composable
private fun policyKindLabel(type: String): String = stringResource(
    when (type) {
        "HEALTH" -> R.string.insurance_cert_kind_health
        "VEHICLE" -> R.string.insurance_cert_kind_vehicle
        "LIFE_TERM" -> R.string.insurance_cert_kind_life_term
        "LIFE_ENDOWMENT" -> R.string.insurance_cert_kind_life_endowment
        "TRAVEL" -> R.string.insurance_cert_kind_travel
        "HOME" -> R.string.insurance_cert_kind_home
        else -> R.string.insurance_cert_kind_other
    },
)

/** Short premium frequency word for the quick-facts "/yr" style suffix. */
@Composable
private fun PremiumFrequency.freqShort(): String = when (this) {
    PremiumFrequency.MONTHLY -> stringResource(R.string.premium_frequency_monthly)
    PremiumFrequency.QUARTERLY -> stringResource(R.string.premium_frequency_quarterly)
    PremiumFrequency.HALF_YEARLY -> stringResource(R.string.premium_frequency_half_yearly)
    PremiumFrequency.YEARLY -> stringResource(R.string.premium_frequency_yearly)
    PremiumFrequency.SINGLE -> stringResource(R.string.premium_frequency_single)
}
