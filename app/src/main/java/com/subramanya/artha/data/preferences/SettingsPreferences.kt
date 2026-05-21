package com.subramanya.artha.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single-source settings store. Phase 1 uses DataStore Preferences for the small
 * amount of cross-session state (just `userName` for now). A full Room Settings
 * table is deferred until later phases need richer settings.
 *
 * DataStore is created via the file-level delegate `Context.dataStore`; this class
 * is a thin typed wrapper around the resulting [DataStore] instance.
 */
class SettingsPreferences(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.dataStore

    val userName: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.USER_NAME].orEmpty() }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "artha_settings")
