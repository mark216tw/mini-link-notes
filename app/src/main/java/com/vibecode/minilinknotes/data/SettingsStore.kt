package com.vibecode.minilinknotes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val displayMode = stringPreferencesKey("display_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val autoFetchTitle = booleanPreferencesKey("auto_fetch_title")
        val sortField = stringPreferencesKey("sort_field")
        val sortAscending = booleanPreferencesKey("sort_ascending")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { preferences ->
            AppSettings(
                displayMode = preferences[Keys.displayMode]
                    ?.let { runCatching { DisplayMode.valueOf(it) }.getOrNull() }
                    ?: DisplayMode.SYSTEM,
                dynamicColor = preferences[Keys.dynamicColor] ?: true,
                autoFetchTitle = preferences[Keys.autoFetchTitle] ?: true,
                sortField = preferences[Keys.sortField]
                    ?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
                    ?: SortField.CREATED_AT,
                sortAscending = preferences[Keys.sortAscending] ?: false,
            )
        }

    suspend fun setDisplayMode(mode: DisplayMode) {
        context.settingsDataStore.edit { it[Keys.displayMode] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setAutoFetchTitle(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoFetchTitle] = enabled }
    }

    suspend fun setSort(field: SortField, ascending: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.sortField] = field.name
            it[Keys.sortAscending] = ascending
        }
    }
}
