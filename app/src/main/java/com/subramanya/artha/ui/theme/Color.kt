package com.subramanya.artha.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// Artha Design Tokens — mirrors docs/Artha-handoff/.../tokens.css
// Dark theme primary. Indian rupee-note color cues + Material 3 slots.
// ─────────────────────────────────────────────────────────────

// Surfaces — deep ink with teal undertone
val Surface0 = Color(0xFF07100D) // outside / scrim
val Surface1 = Color(0xFF0B1612) // base background
val Surface2 = Color(0xFF111D18) // raised card
val Surface3 = Color(0xFF17261F) // dialog / sheet
val Surface4 = Color(0xFF1E3028) // hover / pressed

// Hairlines — replace shadows in dark theme
val Line1 = Color(0x14B0C8BC) // 8% alpha on warm-grey
val Line2 = Color(0x24B0C8BC) // 14%
val Line3 = Color(0x38B0C8BC) // 22%
val LineTeal = Color(0x5214B8A6) // 32% teal-500

// Brand teal
val Teal700 = Color(0xFF0F766E) // brand primary
val Teal500 = Color(0xFF14B8A6) // accent on dark
val Teal300 = Color(0xFF5EEAD4) // hover bright
val Teal900 = Color(0xFF134E48) // container
val Teal950 = Color(0xFF0A2C29) // deep container

// Indian rupee-note accents
val Ochre = Color(0xFFC2841C)
val OchreSoft = Color(0xFFE2A84A)
val Indigo = Color(0xFF6B7BC4)
val IndigoDeep = Color(0xFF3E4B8C)
val Saffron = Color(0xFFE58B2A)
val Terracotta = Color(0xFFC25450)

// Semantic
val Income = Color(0xFF56BD8C) // muted sage
val IncomeSoft = Color(0xFF2E5D45)
val Expense = Color(0xFFE58B6F) // warm coral, never aggressive red
val ExpenseSoft = Color(0xFF5D332A)
val Danger = Color(0xFFD9534F)

// Text
val Text1 = Color(0xFFF0EAD6) // warm off-white, note-paper
val Text2 = Color(0xFFA8B3AC)
val Text3 = Color(0xFF6F7A74)
val Text4 = Color(0xFF4D5752)

// Account / category accents
val AccTeal = Color(0xFF0F766E)
val AccIndigo = Color(0xFF5260A8)
val AccEmerald = Color(0xFF2F8F6B)
val AccSaffron = Color(0xFFC97A2A)
val AccMagenta = Color(0xFFB14A6E)
val AccViolet = Color(0xFF7D5BB8)

// ─────────────────────────────────────────────────────────────
// Theme-aware fills — for dark tokens that have no Material colorScheme slot.
// Dark returns the original token (so dark mode is unchanged); light returns a pale
// counterpart of the same hue so the element reads correctly on a bright surface.
// ─────────────────────────────────────────────────────────────
@Composable @ReadOnlyComposable
fun incomeSoftFill(): Color = if (LocalArthaIsDark.current) IncomeSoft else Color(0xFFD6EEDF)

@Composable @ReadOnlyComposable
fun expenseSoftFill(): Color = if (LocalArthaIsDark.current) ExpenseSoft else Color(0xFFF8DDD2)

/** AI Quick Entry card gradient end (deep teal in dark, pale teal in light). */
@Composable @ReadOnlyComposable
fun aiCardGradientEnd(): Color = if (LocalArthaIsDark.current) Teal950 else Color(0xFFCDEFE8)

// ─────────────────────────────────────────────────────────────
// Legacy aliases — older code still imports these. Map onto the
// new dark palette so screens we haven't rewritten yet stay legible.
// ─────────────────────────────────────────────────────────────
val ArthaTealPrimary = Teal700
val ArthaTealOnPrimary = Color(0xFFFFFFFF)
val ArthaTealPrimaryContainer = Teal900
val ArthaTealOnPrimaryContainer = Teal300

val ArthaTealSecondary = Ochre
val ArthaTealOnSecondary = Color(0xFF1A1208)
val ArthaTealSecondaryContainer = Color(0xFF3A2A14)
val ArthaTealOnSecondaryContainer = OchreSoft

val ArthaTealTertiary = Indigo
val ArthaTealOnTertiary = Color(0xFF0A1228)
val ArthaTealTertiaryContainer = IndigoDeep
val ArthaTealOnTertiaryContainer = Color(0xFFD4DBFA)

val ArthaTealPrimaryDark = Teal300
val ArthaTealOnPrimaryDark = Color(0xFF003732)
val ArthaTealPrimaryContainerDark = Teal900
val ArthaTealOnPrimaryContainerDark = Teal300
