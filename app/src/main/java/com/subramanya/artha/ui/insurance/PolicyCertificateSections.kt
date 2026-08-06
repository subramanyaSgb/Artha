package com.subramanya.artha.ui.insurance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.subramanya.artha.R
import com.subramanya.artha.ui.theme.CormorantGaramond
import com.subramanya.artha.ui.theme.Manrope
import com.subramanya.artha.ui.theme.PlayfairDisplay

/**
 * The "schedule" body sections of the 1:1 policy-certificate insurance detail
 * screen. Every composable here is a self-contained dark card/row matching the
 * design spec; the screen ([InsuranceDetailScreen]) composes them from real data
 * and HIDES each one when its data is empty/null. Nothing here decides what to
 * show — callers gate on data before calling.
 *
 * Colours & text styles come from [CertTokens]. Sizes are the design px→dp / .sp.
 */

// ---------------- 1. Quick facts grid ----------------

/** One 2-col tile in [QuickFactsGrid]. */
enum class QuickFactIcon { CALENDAR, EVENT, GROUP, UMBRELLA, PAYMENTS, VERIFIED }

private fun QuickFactIcon.vector(): ImageVector = when (this) {
    QuickFactIcon.CALENDAR -> Icons.Filled.CalendarMonth
    QuickFactIcon.EVENT -> Icons.Filled.Event
    QuickFactIcon.GROUP -> Icons.Filled.Group
    QuickFactIcon.UMBRELLA -> Icons.Filled.Umbrella
    QuickFactIcon.PAYMENTS -> Icons.Filled.Payments
    QuickFactIcon.VERIFIED -> Icons.Filled.VerifiedUser
}

private data class QuickFact(
    val icon: QuickFactIcon,
    val label: String,
    val value: String,
    val valueSuffix: String? = null,
)

/**
 * 2-column grid of up to 6 fact tiles, divided by hairline borders (right border
 * on left cells, bottom border on non-last rows). Null values are skipped, so the
 * grid compacts for sparse policies.
 */
@Composable
fun QuickFactsGrid(
    started: String?,
    expires: String?,
    members: String?,
    policyType: String?,
    premium: String?,
    premiumFreqSuffix: String?,
    taxBenefit: String?,
    modifier: Modifier = Modifier,
) {
    val lblStarted = stringResource(R.string.insurance_cert_fact_started)
    val lblExpires = stringResource(R.string.insurance_cert_fact_expires)
    val lblMembers = stringResource(R.string.insurance_cert_fact_members)
    val lblPolicyType = stringResource(R.string.insurance_cert_fact_policy_type)
    val lblPremium = stringResource(R.string.insurance_cert_fact_premium)
    val lblTax = stringResource(R.string.insurance_cert_fact_tax_benefit)
    val facts = buildList {
        started?.let { add(QuickFact(QuickFactIcon.CALENDAR, lblStarted, it)) }
        expires?.let { add(QuickFact(QuickFactIcon.EVENT, lblExpires, it)) }
        members?.let { add(QuickFact(QuickFactIcon.GROUP, lblMembers, it)) }
        policyType?.let { add(QuickFact(QuickFactIcon.UMBRELLA, lblPolicyType, it)) }
        premium?.let { add(QuickFact(QuickFactIcon.PAYMENTS, lblPremium, it, premiumFreqSuffix)) }
        taxBenefit?.let { add(QuickFact(QuickFactIcon.VERIFIED, lblTax, it)) }
    }
    if (facts.isEmpty()) return

    val rows = facts.chunked(2)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CertTokens.cardBorder, RoundedCornerShape(16.dp))
            .background(CertTokens.cardBg),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            val isLastRow = rowIndex == rows.lastIndex
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { colIndex, fact ->
                    val isLeftCell = colIndex == 0 && row.size == 2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isLeftCell) {
                                    Modifier.borderRight(CertTokens.cardBorder)
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (!isLastRow) Modifier.borderBottom(CertTokens.cardBorder) else Modifier,
                            ),
                    ) {
                        QuickFactTile(fact)
                    }
                }
                // odd trailing tile → keep the second column slot empty for alignment
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickFactTile(fact: QuickFact) {
    Row(
        modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(CertTokens.gold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = fact.icon.vector(),
                contentDescription = null,
                tint = CertTokens.gold,
                modifier = Modifier.size(15.dp),
            )
        }
        Column {
            Text(fact.label.uppercase(), style = CertTokens.goldMicroLabel)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    fact.value,
                    style = TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.Manrope,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CertTokens.textPrimary,
                    ),
                )
                fact.valueSuffix?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(start = 1.dp),
                        style = TextStyle(
                            fontFamily = com.subramanya.artha.ui.theme.Manrope,
                            fontSize = 10.sp,
                            color = CertTokens.textMuted,
                        ),
                    )
                }
            }
        }
    }
}

// ---------------- 2. Validity card ----------------

/**
 * Cream "validity period" card with a red VALID/EXPIRED stamp and a progress bar
 * for the elapsed fraction of the policy term.
 */
@Composable
fun ValidityCard(
    rangeText: String,
    elapsedFraction: Float,
    elapsedLabel: String,
    remainingLabel: String,
    validStamp: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(CertTokens.validityBorder))
            .padding(2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(CertTokens.cream1, CertTokens.cream2)))
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.insurance_cert_validity_period),
                        style = TextStyle(
                            fontFamily = Manrope,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.15.em,
                            color = CertTokens.greenGreyLabel,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        rangeText,
                        style = TextStyle(
                            fontFamily = com.subramanya.artha.ui.theme.CormorantGaramond,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.02.em,
                            color = CertTokens.greenDeep,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .rotate(-5f)
                        .border(1.5.dp, CertTokens.stampRed, RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        validStamp,
                        style = TextStyle(
                            fontFamily = com.subramanya.artha.ui.theme.PlayfairDisplay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.13.em,
                            color = CertTokens.stampRed,
                        ),
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CertTokens.greenDeep.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(elapsedFraction.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(CertTokens.greenDark, CertTokens.greenMid))),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    elapsedLabel,
                    style = TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.Manrope,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                        color = CertTokens.doubleBorderGreen,
                    ),
                )
                Text(
                    remainingLabel,
                    style = TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.Manrope,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                        color = CertTokens.stampRed,
                    ),
                )
            }
        }
    }
}

// ---------------- 3. Schedule divider ----------------

@Composable
fun ScheduleDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, CertTokens.dividerHeavy))),
        )
        Text(text, style = CertTokens.sectionHeaderStyle)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(CertTokens.dividerHeavy, Color.Transparent))),
        )
    }
}

// ---------------- 4. Accordion section ----------------

@Composable
fun AccordionSection(
    numeral: String,
    title: String,
    initiallyOpen: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember { mutableStateOf(initiallyOpen) }
    val chevronRotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CertTokens.cardBorder, RoundedCornerShape(14.dp))
            .background(CertTokens.cardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .border(1.dp, CertTokens.numeralChipBorder, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    numeral,
                    style = TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.Manrope,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CertTokens.gold,
                    ),
                )
            }
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CertTokens.textPrimary,
                ),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = CertTokens.labelMuted,
                modifier = Modifier.rotate(chevronRotation),
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, bottom = 6.dp),
                content = content,
            )
        }
    }
}

// ---------------- 5. Meta row ----------------

@Composable
fun MetaRow(label: String, value: String, valueColor: Color = CertTokens.textPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .borderTop(CertTokens.dividerThin)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = CertTokens.dataRowLabel, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = CertTokens.dataRowValue.copy(color = valueColor),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------- 6. Member row ----------------

@Composable
fun MemberRow(
    initials: String,
    name: String,
    sub: String?,
    avatarGradient: List<Color>,
    proposer: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, CertTokens.rowBorder, RoundedCornerShape(11.dp))
            .background(CertTokens.rowBg)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(avatarGradient)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEFE3C7),
                ),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CertTokens.textPrimary,
                ),
            )
            sub?.let {
                Text(
                    it,
                    style = TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.Manrope,
                        fontSize = 11.sp,
                        color = CertTokens.labelMuted,
                    ),
                )
            }
        }
        if (proposer) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, CertTokens.gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    stringResource(R.string.insurance_cert_proposer),
                    style = TextStyle(
                        fontFamily = Manrope,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.09.em,
                        color = CertTokens.gold,
                    ),
                )
            }
        }
    }
}

// ---------------- 7. Rider row ----------------

@Composable
fun RiderRow(name: String, premium: String?, note: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, CertTokens.rowBorder, RoundedCornerShape(11.dp))
            .background(CertTokens.rowBg)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                name,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CertTokens.textPrimary,
                ),
            )
            premium?.let {
                Text(
                    it,
                    style = TextStyle(
                        fontFamily = com.subramanya.artha.ui.theme.Manrope,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CertTokens.gold,
                    ),
                )
            }
        }
        note?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 4.dp),
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 11.sp,
                    color = CertTokens.labelMuted,
                ),
            )
        }
    }
}

// ---------------- 8. Exclusion row ----------------

@Composable
fun ExclusionRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .borderTop(CertTokens.dividerThin)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("◆", style = TextStyle(fontSize = 11.sp, color = CertTokens.terracotta))
        Text(
            text,
            style = TextStyle(
                fontFamily = com.subramanya.artha.ui.theme.Manrope,
                fontSize = 12.sp,
                lineHeight = 18.6.sp, // ~1.55 of 12
                color = CertTokens.textOnExcl,
            ),
        )
    }
}

// ---------------- 9. Contact row ----------------

@Composable
fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, CertTokens.rowBorder, RoundedCornerShape(11.dp))
            .background(CertTokens.rowBg)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(CertTokens.gold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = CertTokens.gold, modifier = Modifier.size(15.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), style = CertTokens.goldMicroLabel)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CertTokens.textPrimary,
                ),
            )
        }
        trailingAction?.invoke()
    }
}

// ---------------- 10. Premium total row ----------------

@Composable
fun PremiumTotalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .borderTop(CertTokens.dividerHeavy)
            .borderBottom(CertTokens.dividerHeavy)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = com.subramanya.artha.ui.theme.Manrope,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = CertTokens.textPrimary,
            ),
        )
        Text(
            value,
            style = TextStyle(
                fontFamily = com.subramanya.artha.ui.theme.Manrope,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CertTokens.gold,
            ),
        )
    }
}

// ---------------- 11. Documents card ----------------

@Composable
fun DocumentsRow(label: String, onViewPdf: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, CertTokens.rowBorder, RoundedCornerShape(11.dp))
            .background(CertTokens.rowBg)
            .clickable(onClick = onViewPdf)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(CertTokens.gold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PictureAsPdf,
                contentDescription = null,
                tint = CertTokens.gold,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontFamily = com.subramanya.artha.ui.theme.Manrope,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = CertTokens.textPrimary,
            ),
        )
    }
}

// ---------------- 12. Renewal reminder card ----------------

/**
 * ponytail: the design's toggle switch is decorative (no scheduling wired here —
 * renewal reminders come from nextPremiumDate + WorkManager elsewhere). Rendered
 * as a static informational card without the switch.
 */
@Composable
fun RenewalReminderCard(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, CertTokens.reminderBorder, RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(CertTokens.reminderBgStart, CertTokens.cardBg)))
            .padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CertTokens.successGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Event,
                contentDescription = null,
                tint = CertTokens.successGreen,
                modifier = Modifier.size(17.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.insurance_cert_reminder_title),
                style = CertTokens.goldMicroLabel.copy(color = CertTokens.successGreen),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text,
                style = TextStyle(
                    fontFamily = com.subramanya.artha.ui.theme.Manrope,
                    fontSize = 12.sp,
                    color = CertTokens.textPrimary,
                ),
            )
        }
    }
}

// ---------------- 13. Footer disclaimer ----------------

@Composable
fun FooterDisclaimer(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = com.subramanya.artha.ui.theme.Manrope,
            fontSize = 10.sp,
            lineHeight = 16.sp, // ~1.6
            color = CertTokens.footerGrey,
        ),
    )
}

// ---------------- helpers ----------------

/** Assigns an avatar gradient by index, cycling green→purple→goldBrown. */
fun avatarGradientFor(index: Int): List<Color> = when (index % 3) {
    0 -> CertTokens.avatarGreen
    1 -> CertTokens.avatarPurple
    else -> CertTokens.avatarGoldBrown
}

/** Initials (up to 2) from a person's name. */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }

// Single-edge hairlines — Compose has no per-side border, so draw a 1.dp slice.
private fun Modifier.borderRight(color: Color): Modifier = drawBehind {
    val w = 1.dp.toPx()
    drawRect(color, topLeft = Offset(size.width - w, 0f), size = Size(w, size.height))
}

private fun Modifier.borderBottom(color: Color): Modifier = drawBehind {
    val h = 1.dp.toPx()
    drawRect(color, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
}

private fun Modifier.borderTop(color: Color): Modifier = drawBehind {
    val h = 1.dp.toPx()
    drawRect(color, topLeft = Offset(0f, 0f), size = Size(size.width, h))
}
