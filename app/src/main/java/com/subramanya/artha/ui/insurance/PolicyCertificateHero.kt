package com.subramanya.artha.ui.insurance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import com.subramanya.artha.ui.theme.CormorantGaramond
import com.subramanya.artha.ui.theme.Manrope
import com.subramanya.artha.ui.theme.MrsSaintDelafield
import com.subramanya.artha.ui.theme.PlayfairDisplay
import kotlin.math.tan

/**
 * 1:1 replica of the ornate insurance "policy certificate" hero card.
 *
 * Structure is four nested layers, outside-in:
 *   A  gold-foil frame  (linear-gradient bezel + drop shadow)
 *   B  paper            (radial cream gradient + guilloché texture + vignette)
 *   C  double border    (two concentric green rings — Compose has no double border)
 *   D  inner hairline    (thin ring + top highlight + 4 corner fleurons)
 *
 * Every dynamic value comes from a param; a null/blank param hides its whole
 * sub-block so sparse policies degrade gracefully. planName + sumInsured always
 * render (the caller guarantees them). The structural LABELS (INSURER, SUM
 * INSURED, POLICY NUMBER, …) are design chrome, kept as literals here — this is
 * a one-off ornate certificate, not translatable app copy.
 *
 * Notes on judgment calls the task left open:
 *  - "Mr." prefix: always prefixed (we can't infer gender). See [lifeAssured].
 *  - Double border: drawn as two concentric rounded-rect rings via drawBehind.
 *  - Seal text: insurer initials + issue year (derived, generic embossed seal).
 */
@Composable
fun PolicyCertificateHero(
    insurer: String, // "Meridian Health Insurance Ltd."
    statusLabel: String?, // "ACTIVE" (null → hide chip)
    planName: String, // "CARE SUPREME"
    policyKind: String?, // "HEALTH INSURANCE POLICY" (null → hide)
    sumInsuredFormatted: String, // "₹1,00,00,000" (already Indian-formatted)
    sumInsuredWords: String?, // "ONE CRORE ONLY" (null → hide)
    policyNumber: String?, // "92838249"
    issuedOn: String?, // "22 NOV 2024"
    lifeAssured: String?, // "Gopala Krishnan" → cursive "Mr. <name>"
    uin: String?, // "MHIHLIP24063V012425"
    modifier: Modifier = Modifier,
) {
    // LAYER A — gold-foil frame
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = CertTokens.heroGoldFoil,
                    // ~160° sweep: start top-left, end bottom-right-ish
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY),
                ),
            )
            .padding(7.dp),
    ) {
        // LAYER B — paper (radial cream) + textures
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to CertTokens.paperR1,
                            0.45f to CertTokens.paperR2,
                            1.0f to CertTokens.paperR3,
                        ),
                        // center ~18% / 0%
                        center = Offset(0.18f * 1000f, 0f),
                        radius = 1400f,
                    ),
                )
                .guillocheOverlay()
                .padding(3.dp),
        ) {
            // LAYER C — double border (two concentric green rings)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .doubleBorder(RoundedCornerShape(6.dp)),
            ) {
                // LAYER D — inner hairline + top highlight + corner fleurons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(
                            1.dp,
                            CertTokens.doubleBorderGreen.copy(alpha = 0.55f),
                            RoundedCornerShape(3.dp),
                        )
                        // top white highlight, fading out by ~30%
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.White.copy(alpha = 0.28f),
                                        0.30f to Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = size.height,
                                ),
                            )
                        }
                        .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    HeroContent(
                        insurer = insurer,
                        statusLabel = statusLabel,
                        planName = planName,
                        policyKind = policyKind,
                        sumInsuredFormatted = sumInsuredFormatted,
                        sumInsuredWords = sumInsuredWords,
                        policyNumber = policyNumber,
                        issuedOn = issuedOn,
                        lifeAssured = lifeAssured,
                        uin = uin,
                    )

                    // 4 corner fleurons (❦)
                    CornerFleuron(Modifier.align(Alignment.TopStart), x = 3.dp, y = 3.dp)
                    CornerFleuron(Modifier.align(Alignment.TopEnd), x = (-3).dp, y = 3.dp)
                    CornerFleuron(Modifier.align(Alignment.BottomStart), x = 3.dp, y = (-3).dp)
                    CornerFleuron(Modifier.align(Alignment.BottomEnd), x = (-3).dp, y = (-3).dp)
                }
            }
        }
    }
}

@Composable
private fun HeroContent(
    insurer: String,
    statusLabel: String?,
    planName: String,
    policyKind: String?,
    sumInsuredFormatted: String,
    sumInsuredWords: String?,
    policyNumber: String?,
    issuedOn: String?,
    lifeAssured: String?,
    uin: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "INSURER",
                    style = TextStyle(
                        fontFamily = Manrope,
                        fontSize = 6.4.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.em,
                        color = CertTokens.greenGreyLabel,
                    ),
                )
                Text(
                    text = insurer,
                    style = TextStyle(
                        fontFamily = PlayfairDisplay,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CertTokens.greenDark,
                        lineHeight = 15.5.sp, // ~1.15 of 13.5
                    ),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "POLICY STATUS",
                    style = TextStyle(
                        fontFamily = Manrope,
                        fontSize = 7.sp,
                        letterSpacing = 0.16.em,
                        color = CertTokens.greenGreyLabel,
                    ),
                )
                if (!statusLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .border(
                                1.dp,
                                CertTokens.greenDark,
                                RoundedCornerShape(3.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = statusLabel,
                            style = TextStyle(
                                fontFamily = Manrope,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.14.em,
                                color = CertTokens.greenDark,
                            ),
                        )
                    }
                }
            }
        }

        // 2. Plan title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "❧", // ❧
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = CertTokens.greenMuted.copy(alpha = 0.8f),
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = planName.uppercase(),
                    style = TextStyle(
                        fontFamily = PlayfairDisplay,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.04.em,
                        color = CertTokens.greenDeep,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "❧", // ❧ mirrored
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = CertTokens.greenMuted.copy(alpha = 0.8f),
                    ),
                )
            }
            if (!policyKind.isNullOrBlank()) {
                Text(
                    text = policyKind.uppercase(),
                    modifier = Modifier.padding(top = 6.dp),
                    style = TextStyle(
                        fontFamily = PlayfairDisplay,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.13.em,
                        color = CertTokens.greenMuted,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 3. Sum insured
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SUM INSURED",
                style = TextStyle(
                    fontFamily = Manrope,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.14.em,
                    color = CertTokens.greenMuted,
                ),
            )
            Text(
                text = sumInsuredFormatted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontFamily = PlayfairDisplay,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CertTokens.greenDeep,
                ),
            )
            if (!sumInsuredWords.isNullOrBlank()) {
                Text(
                    text = sumInsuredWords,
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontSize = 10.sp,
                        letterSpacing = 0.1.em,
                        color = CertTokens.greenMuted,
                    ),
                )
            }
        }

        // 4. Two-column row: policy number / issued on
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            CertFieldColumn(
                modifier = Modifier.weight(1f),
                label = "POLICY NUMBER",
                value = policyNumber,
                alignEnd = false,
            )
            CertFieldColumn(
                modifier = Modifier.weight(1f),
                label = "ISSUED ON",
                value = issuedOn,
                alignEnd = true,
            )
        }

        // 5. Attestation box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(3.dp))
                .border(
                    1.dp,
                    Color(0xFF7A6030).copy(alpha = 0.28f), // rgba(122,96,48,0.28)
                    RoundedCornerShape(3.dp),
                )
                .padding(start = 14.dp, top = 12.dp, end = 10.dp, bottom = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "This is to certify that",
                    style = TextStyle(
                        fontFamily = CormorantGaramond,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = CertTokens.greenMuted,
                    ),
                )
                if (!lifeAssured.isNullOrBlank()) {
                    Text(
                        text = "Mr. $lifeAssured",
                        style = TextStyle(
                            fontFamily = MrsSaintDelafield,
                            fontSize = 40.sp,
                            color = CertTokens.greenDeep,
                            lineHeight = 42.sp, // ~1.05
                        ),
                    )
                }

                // dotted rule
                DottedRule(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                )

                // Signature footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    EmbossedSeal(insurer = insurer, issuedOn = issuedOn)

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = insurer.trim().substringBefore(' '),
                            style = TextStyle(
                                fontFamily = MrsSaintDelafield,
                                fontSize = 30.sp,
                                color = CertTokens.greenDeep,
                            ),
                        )
                        DottedRule(
                            modifier = Modifier
                                .width(120.dp)
                                .padding(vertical = 4.dp),
                        )
                        Text(
                            text = "Authorised Signatory",
                            style = TextStyle(
                                fontFamily = Manrope,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CertTokens.greenMuted,
                            ),
                        )
                        Text(
                            text = insurer,
                            style = TextStyle(
                                fontFamily = Manrope,
                                fontSize = 7.5.sp,
                                color = CertTokens.greenGreyLabel,
                            ),
                        )
                    }
                }
            }
        }

        // 6. Footer meta row
        if (!uin.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "UIN: $uin",
                    style = footerMetaStyle,
                )
            }
        }
    }
}

/** A label + dotted-underlined value column (used for policy number / issued on). */
@Composable
private fun CertFieldColumn(
    modifier: Modifier,
    label: String,
    value: String?,
    alignEnd: Boolean,
) {
    if (value.isNullOrBlank()) {
        // keep the column slot but empty, so the sibling stays put
        Column(modifier = modifier) {}
        return
    }
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Manrope,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.em,
                color = CertTokens.greenGreyLabel,
            ),
        )
        Text(
            text = value,
            modifier = Modifier
                .padding(top = 2.dp)
                .drawBehind {
                    val y = size.height + 3.dp.toPx()
                    drawLine(
                        color = CertTokens.dottedGoldBrown,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), 3.dp.toPx()),
                        ),
                    )
                },
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CertTokens.stampRed,
            ),
        )
    }
}

/** Round embossed seal with insurer initials + issue year, tilted -11°. */
@Composable
private fun EmbossedSeal(insurer: String, issuedOn: String?) {
    val initials = insurer.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "•" }
    val year = issuedOn?.let { Regex("\\d{4}").find(it)?.value }

    Box(
        modifier = Modifier
            .size(70.dp)
            .rotate(-11f)
            .clip(CircleShape)
            .border(1.5.dp, CertTokens.sealPurple, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = initials,
                style = TextStyle(
                    fontFamily = Manrope,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.1.em,
                    color = CertTokens.sealPurple.copy(alpha = 0.72f),
                ),
            )
            if (!year.isNullOrBlank()) {
                Text(
                    text = year,
                    style = TextStyle(
                        fontFamily = Manrope,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.em,
                        color = CertTokens.sealPurple.copy(alpha = 0.72f),
                    ),
                )
            }
        }
    }
}

/** A corner fleuron glyph (❦) offset by [x]/[y] from the aligned corner. */
@Composable
private fun CornerFleuron(modifier: Modifier, x: Dp, y: Dp) {
    Text(
        text = "❦", // ❦
        modifier = modifier.padding(
            start = if (x >= 0.dp) x else 0.dp,
            end = if (x < 0.dp) -x else 0.dp,
            top = if (y >= 0.dp) y else 0.dp,
            bottom = if (y < 0.dp) -y else 0.dp,
        ),
        style = TextStyle(
            fontSize = 13.sp,
            color = CertTokens.doubleBorderGreen.copy(alpha = 0.75f),
        ),
    )
}

/** Horizontal dotted rule in the gold-brown accent. */
@Composable
private fun DottedRule(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = CertTokens.dottedGoldBrown,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(2.dp.toPx(), 3.dp.toPx()),
                    ),
                )
            },
    )
}

private val footerMetaStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = 7.5.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.07.em,
    color = CertTokens.greenGreyLabel,
)

/**
 * Two concentric green rings with a ~2px gap — Compose has no "double" border,
 * so this is two rounded-rect strokes drawn behind the content.
 */
private fun Modifier.doubleBorder(shape: RoundedCornerShape): Modifier =
    this
        .border(1.dp, CertTokens.doubleBorderGreen, shape)
        .padding(3.dp)
        .border(1.dp, CertTokens.doubleBorderGreen, RoundedCornerShape(4.dp))
        .padding(1.dp)

/**
 * Subtle guilloché texture: thin diagonal cross-hatch at ±38° plus an edge
 * vignette. Kept very faint (alpha ~0.06) so it reads as engraved paper.
 */
private fun Modifier.guillocheOverlay(): Modifier = this.drawBehind {
    val lineColor = Color(0xFF7A6030).copy(alpha = 0.06f)
    val stroke = 1f
    val spacing = 5.dp.toPx()
    val w = size.width
    val h = size.height
    // +38° lines: slope = tan(38°). Offset the intercept in steps.
    val slopePos = tan(Math.toRadians(38.0)).toFloat()
    val slopeNeg = -slopePos
    // Draw a family of lines y = slope*x + b, stepping b to cover the box.
    val span = (h + w * kotlin.math.abs(slopePos))
    var b = -span
    while (b < span) {
        // +38°
        drawLine(
            color = lineColor,
            start = Offset(0f, b),
            end = Offset(w, slopePos * w + b),
            strokeWidth = stroke,
        )
        // -38°
        drawLine(
            color = lineColor,
            start = Offset(0f, b),
            end = Offset(w, slopeNeg * w + b),
            strokeWidth = stroke,
        )
        b += spacing
    }
    // edge vignette — darken corners
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.7f to Color.Transparent,
                1.0f to Color(0xFF5C4824).copy(alpha = 0.16f), // rgba(92,72,36,0.16)
            ),
            center = Offset(w / 2f, h / 2f),
            radius = kotlin.math.max(w, h) * 0.75f,
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A, widthDp = 380)
@Composable
private fun PolicyCertificateHeroPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        PolicyCertificateHero(
            insurer = "Meridian Health Insurance Ltd.",
            statusLabel = "ACTIVE",
            planName = "Care Supreme",
            policyKind = "Health Insurance Policy",
            sumInsuredFormatted = "₹1,00,00,000",
            sumInsuredWords = "ONE CRORE ONLY",
            policyNumber = "92838249",
            issuedOn = "22 NOV 2024",
            lifeAssured = "Gopala Krishnan",
            uin = "MHIHLIP24063V012425",
        )
    }
}
