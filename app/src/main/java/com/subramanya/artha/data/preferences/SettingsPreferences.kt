package com.subramanya.artha.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "artha_settings")
