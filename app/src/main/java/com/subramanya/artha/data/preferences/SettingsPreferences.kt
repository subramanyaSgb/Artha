package com.subramanya.artha.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** User-visible theme mode toggle. SYSTEM follows the device's day/night setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Permanent default for the spouse-prompt dialog. ASK means show the dialog every
 * time. The dialog itself is deferred; this preference is forward-compatible so
 * the wiring is already in place when the dialog lands.
 */
enum class SpouseTransactionDefault { ASK, TRANSFER, EXPENSE }

/**
 * Single-source settings store. Phase 1 keeps a small DataStore Preferences set;
 * a full Room Settings table is deferred until later phases need richer state.
 */
class SettingsPreferences(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.dataStore

    val userName: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.USER_NAME].orEmpty() }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.USE_DYNAMIC_COLOR] ?: true
    }

    val spouseTransactionDefault: Flow<SpouseTransactionDefault> = dataStore.data.map { prefs ->
        prefs[Keys.SPOUSE_DEFAULT]?.let { runCatching { SpouseTransactionDefault.valueOf(it) }.getOrNull() }
            ?: SpouseTransactionDefault.ASK
    }

    // Dashboard section visibility — Net Position hero always shows; everything else is opt-out.
    val dashboardShowMonthly: Flow<Boolean> = dataStore.data.map { it[Keys.DASH_SHOW_MONTHLY] ?: true }
    val dashboardShowAccounts: Flow<Boolean> = dataStore.data.map { it[Keys.DASH_SHOW_ACCOUNTS] ?: true }
    val dashboardShowCards: Flow<Boolean> = dataStore.data.map { it[Keys.DASH_SHOW_CARDS] ?: true }
    val dashboardShowRecent: Flow<Boolean> = dataStore.data.map { it[Keys.DASH_SHOW_RECENT] ?: true }

    suspend fun setDashboardShowMonthly(value: Boolean) {
        dataStore.edit { it[Keys.DASH_SHOW_MONTHLY] = value }
    }

    suspend fun setDashboardShowAccounts(value: Boolean) {
        dataStore.edit { it[Keys.DASH_SHOW_ACCOUNTS] = value }
    }

    suspend fun setDashboardShowCards(value: Boolean) {
        dataStore.edit { it[Keys.DASH_SHOW_CARDS] = value }
    }

    suspend fun setDashboardShowRecent(value: Boolean) {
        dataStore.edit { it[Keys.DASH_SHOW_RECENT] = value }
    }

    /** Highest DB schema version that the bundled bank-statement importer has run for.
     *  When the DB version bumps (e.g. v1 -> v2), the splash will re-run the importer
     *  to repopulate data wiped by destructive migration. Surviving "Reset All Data"
     *  is intentional — reset wipes Room only, this stays set, so the user truly gets
     *  a clean slate. Wiped only on uninstall/reinstall. */
    val bundledImportVersion: Flow<Int> = dataStore.data.map { it[Keys.BUNDLED_IMPORT_VERSION] ?: 0 }
    suspend fun setBundledImportVersion(value: Int) {
        dataStore.edit { it[Keys.BUNDLED_IMPORT_VERSION] = value }
    }

    /** Phase 5 — biometric/device-credential lock on app open. Default off so a fresh
     *  install doesn't surprise the user with a prompt. The prompt is checked once per
     *  process launch (not on every screen) per PRD §7.21 "auto-lock timeout". */
    val biometricLockEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.BIOMETRIC_LOCK] ?: false }
    suspend fun setBiometricLockEnabled(value: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_LOCK] = value }
    }

    /** Phase 5 — SMS auto-import toggle. Off by default; flipping it on prompts the
     *  user for RECEIVE_SMS / READ_SMS at runtime. The receiver only runs while this
     *  is true so revoking permission also flips this off. */
    val smsAutoImportEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SMS_AUTO_IMPORT] ?: false }
    suspend fun setSmsAutoImportEnabled(value: Boolean) {
        dataStore.edit { it[Keys.SMS_AUTO_IMPORT] = value }
    }

    /** Master on/off for AI Quick Entry (the "Quick add with Gemini" card on the Dashboard).
     *  Default OFF — the feature stays hidden until the user explicitly turns it on in Settings.
     *  Independent of [geminiApiKey]: the user can enable the toggle and then add a key. */
    val aiQuickEntryEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.AI_QUICK_ENTRY_ENABLED] ?: false }
    suspend fun setAiQuickEntryEnabled(value: Boolean) {
        dataStore.edit { it[Keys.AI_QUICK_ENTRY_ENABLED] = value }
    }

    /** User-supplied Gemini API key for AI Quick Entry. Empty = AI disabled.
     *  Stored only locally (never synced). The Settings flow validates a new key
     *  against the Gemini endpoint before persisting it — see SettingsViewModel. */
    val geminiApiKey: Flow<String> = dataStore.data.map { it[Keys.GEMINI_API_KEY].orEmpty() }
    suspend fun setGeminiApiKey(value: String) {
        dataStore.edit { it[Keys.GEMINI_API_KEY] = value.trim() }
    }
    suspend fun clearGeminiApiKey() {
        dataStore.edit { it.remove(Keys.GEMINI_API_KEY) }
    }

    // ----- Configurable cosmetic pick-lists (Phase 1: colours + icons; no schema change since
    //        category/tag rows already store color:Long and icon:String). Stored as a delimited
    //        string of the user's ADDED entries; the form merges them after the built-in set. -----

    /** Extra colour swatches the user added, in pick order. */
    val customColours: Flow<List<Long>> = dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_COLOURS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
    }

    suspend fun addCustomColour(color: Long) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_COLOURS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
            if (color !in current) prefs[Keys.CUSTOM_COLOURS] = (current + color).joinToString(",")
        }
    }

    suspend fun removeCustomColour(color: Long) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_COLOURS]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
            prefs[Keys.CUSTOM_COLOURS] = (current - color).joinToString(",")
        }
    }

    /** Extra icon keys the user added, in pick order. */
    val customIcons: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_ICONS]?.split(',')?.filter { it.isNotBlank() }.orEmpty()
    }

    suspend fun addCustomIcon(key: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_ICONS]?.split(',')?.filter { it.isNotBlank() }.orEmpty()
            if (key !in current) prefs[Keys.CUSTOM_ICONS] = (current + key).joinToString(",")
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSpouseTransactionDefault(value: SpouseTransactionDefault) {
        dataStore.edit { it[Keys.SPOUSE_DEFAULT] = value.name }
    }

    /** Used by Settings → "Reset spouse prompt" to drop the saved default. */
    suspend fun resetSpouseTransactionDefault() {
        dataStore.edit { it[Keys.SPOUSE_DEFAULT] = SpouseTransactionDefault.ASK.name }
    }

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SPOUSE_DEFAULT = stringPreferencesKey("spouse_transaction_default")
        val DASH_SHOW_MONTHLY = booleanPreferencesKey("dashboard_show_monthly")
        val DASH_SHOW_ACCOUNTS = booleanPreferencesKey("dashboard_show_accounts")
        val DASH_SHOW_CARDS = booleanPreferencesKey("dashboard_show_cards")
        val DASH_SHOW_RECENT = booleanPreferencesKey("dashboard_show_recent")
        val BUNDLED_IMPORT_VERSION = intPreferencesKey("bundled_import_version")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        val SMS_AUTO_IMPORT = booleanPreferencesKey("sms_auto_import_enabled")
        val AI_QUICK_ENTRY_ENABLED = booleanPreferencesKey("ai_quick_entry_enabled")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val CUSTOM_COLOURS = stringPreferencesKey("custom_colours")
        val CUSTOM_ICONS = stringPreferencesKey("custom_icons")
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "artha_settings")
