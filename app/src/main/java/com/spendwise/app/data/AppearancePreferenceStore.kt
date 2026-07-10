package com.spendwise.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearancePreferencesDataStore by preferencesDataStore(name = "appearance_preferences")

class AppearancePreferenceStore(
    private val context: Context
) {
    val isDarkMode: Flow<Boolean> = context.appearancePreferencesDataStore.data.map { preferences ->
        AppearancePreferenceMapper.toDarkMode(preferences[DARK_MODE_ENABLED])
    }

    val startupDarkModePreference: Flow<Boolean?> =
        context.appearancePreferencesDataStore.data.map { preferences ->
            AppearancePreferenceMapper.toStartupPreference(preferences[DARK_MODE_ENABLED])
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.appearancePreferencesDataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    private companion object {
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    }
}
