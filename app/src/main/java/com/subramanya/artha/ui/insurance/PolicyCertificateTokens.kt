package com.subramanya.artha.ui.insurance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.subramanya.artha.ui.theme.Manrope
import com.subramanya.artha.ui.theme.PlayfairDisplay

/**
 * Design palette + reusable text-style tokens for the ornate insurance
 * "policy certificate" detail screen. The certificate UI (later tasks) consumes
 * these; nothing here renders anything on its own.
 *
 * Colours are the EXACT design hexes (0xFF alpha). Gradient stop lists are plain
 * [Color] lists the UI wraps in Brush.linearGradient/verticalGradient as needed.
 */
object CertTokens {
    // Base / dark
    val pageBg = Color(0xFF08090A)
    val cardBg = Color(0xFF121415)
    val cardBorder = Color(0xFF1E2123)
    // Slightly brighter border used on lifted dark cards (accordions, quick-facts grid)
    // so they read as raised on the near-black page bg instead of a muddy mass.
    val cardBorderLifted = Color(0xFF24272A)
    val rowBg = Color(0xFF171A1C)
    val rowBorder = Color(0xFF22262A)
    val dividerThin = Color(0xFF1B1E20)
    val dividerHeavy = Color(0xFF2A2D30)
    val numeralChipBorder = Color(0xFF2C3033)

    // Gold
    val gold = Color(0xFFD8B45C)
    val goldLight = Color(0xFFEFD08A)
    val ctaGoldLight = Color(0xFFE6D3A6)
    val ctaGoldDeep = Color(0xFFC9A85F)

    // Cream / paper
    val cream1 = Color(0xFFF4EAD4)
    val cream2 = Color(0xFFE5D7B7)
    val paperR1 = Color(0xFFF7EEDA)
    val paperR2 = Color(0xFFEFE3C7)
    val paperR3 = Color(0xFFE2D3B2)

    // Green
    val greenDeep = Color(0xFF16382A)
    val greenDark = Color(0xFF1F4634)
    val greenMid = Color(0xFF4A7A57)
    val greenMuted = Color(0xFF3B4A34)
    val greenMuted2 = Color(0xFF33422D)
    val doubleBorderGreen = Color(0xFF4A5A3E)
    val greenGreyLabel = Color(0xFF6B7A5C)
    val successGreen = Color(0xFF7DCB9A)
    val reminderBgStart = Color(0xFF14251C)
    val reminderBorder = Color(0xFF24382C)
    val reminderToggleOn = Color(0xFF37795A)

    // Accent
    val stampRed = Color(0xFF8A2B22)
    val dottedGoldBrown = Color(0xFFA08C60)
    val sealPurple = Color(0xFF4A3E7A)
    val sealPurpleLight = Color(0xFF6B5CA8)
    val terracotta = Color(0xFFC0785E)

    // Text
    val textPrimary = Color(0xFFF2EEE4)
    val textOnExcl = Color(0xFFC6C9CD)
    val labelMuted = Color(0xFF8A8F94)
    val textMuted = Color(0xFF7E8388)
    val footerGrey = Color(0xFF62666A)

    // Gradients (stop lists the UI turns into Brushes)
    val heroGoldFoil = listOf(
        Color(0xFFE4D5B4),
        Color(0xFFC2AE84),
        Color(0xFFE8DCC0),
        Color(0xFFB9A374),
    )
    val validityBorder = listOf(
        Color(0xFFD8C69E),
        Color(0xFFB9A374),
        Color(0xFFE0D2B0),
    )
    val avatarGreen = listOf(Color(0xFF1F4634), Color(0xFF2F6B4C))
    val avatarPurple = listOf(Color(0xFF4A3E7A), Color(0xFF6B5CA8))
    val avatarGoldBrown = listOf(Color(0xFF7A5A22), Color(0xFFA8823A))

    // Reusable text styles (callers uppercase text where noted)
    val goldMicroLabel = TextStyle(
        fontFamily = Manrope,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.13.em,
        color = gold,
    ) // callers uppercase the text

    val dataRowLabel = TextStyle(
        fontFamily = Manrope,
        fontSize = 12.sp,
        color = labelMuted,
    )

    val dataRowValue = TextStyle(
        fontFamily = Manrope,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = textPrimary,
    )

    val sectionHeaderStyle = TextStyle(
        fontFamily = PlayfairDisplay,
        fontSize = 12.sp,
        letterSpacing = 0.19.em,
        color = gold,
    )
}
