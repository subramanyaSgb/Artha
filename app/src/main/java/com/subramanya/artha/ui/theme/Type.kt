package com.subramanya.artha.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ArthaTypography = Typography()

/**
 * Amount/number text styles with `tnum` (tabular figures) enabled — every digit takes
 * the same width so ₹1,00,000.00 lined up over ₹50.00 stays right-aligned without
 * visible wobble. Use these wherever currency or numerical totals are displayed.
 */
object ArthaAmountStyles {
    private const val TABULAR_FIGURES = "tnum"

    val display: TextStyle =
        TextStyle(
            fontSize = 36.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    val title: TextStyle =
        TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = TABULAR_FIGURES,
        )

    val body: TextStyle =
        TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = TABULAR_FIGURES,
        )
}
