package com.subramanya.artha.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.subramanya.artha.R

/**
 * Bundled (res/font) font families for the ornate insurance "policy certificate"
 * screen. These are shipped in the APK, not fetched via the GMS downloadable-font
 * provider (that's [Fonts.kt], left untouched).
 *
 * playfair_display_bold, manrope, and cormorant_garamond* are VARIABLE fonts
 * (wght axis) — a single file covers all weights and Compose selects the weight
 * from the TextStyle's FontWeight. So no per-weight Font() entries are needed.
 */
val PlayfairDisplay = FontFamily(Font(R.font.playfair_display_bold)) // variable wght; used at 600-800

val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond),
    Font(R.font.cormorant_garamond_italic, style = FontStyle.Italic),
)

val MrsSaintDelafield = FontFamily(Font(R.font.mrs_saint_delafield)) // cursive signature

val Manrope = FontFamily(Font(R.font.manrope)) // sans default for the certificate screen
