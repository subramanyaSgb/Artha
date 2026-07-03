package com.subramanya.artha.data.backup

/**
 * Snapshot of the user's DataStore settings for backup schema v2 ("export
 * everything in one go"). Pure data — enums/lists travel as the same strings
 * DataStore stores so the codec stays dumb and tolerant.
 *
 * Deliberately EXCLUDED:
 *  - Gemini API key — a per-install, revocable secret; never leaves the device.
 *  - bundledImportVersion — install-local bookkeeping, meaningless elsewhere.
 */
data class BackupSettings(
    val userName: String = "",
    val themeMode: String = "SYSTEM",
    val useDynamicColor: Boolean = true,
    val spouseTransactionDefault: String = "ASK",
    val dashboardShowMonthly: Boolean = true,
    val dashboardShowAccounts: Boolean = true,
    val dashboardShowCards: Boolean = true,
    val dashboardShowRecent: Boolean = true,
    val dashboardShowSpending: Boolean = true,
    /** Comma-joined section keys; empty = user never customised the order. */
    val dashboardSectionOrder: String = "",
    val biometricLockEnabled: Boolean = false,
    val smsAutoImportEnabled: Boolean = false,
    val aiQuickEntryEnabled: Boolean = false,
    /** Comma-joined ARGB longs / icon keys the user added to the pickers. */
    val customColours: String = "",
    val customIcons: String = "",
)
