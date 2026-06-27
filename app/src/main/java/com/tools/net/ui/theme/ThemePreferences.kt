package com.tools.net.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

object ThemePreferences {
    fun themeModeFlow(context: Context) = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(context: Context, mode: AppThemeMode) {
        context.themeDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }
}
