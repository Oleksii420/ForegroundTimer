package com.example.waterreminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.waterDataStore: DataStore<Preferences> by preferencesDataStore(name = "water_settings")

object WaterKeys {
    val REMINDER_COUNT = intPreferencesKey("reminder_count")
    val ENABLED = booleanPreferencesKey("reminder_enabled")
    val INTERVAL = longPreferencesKey("interval_minutes")
}

class WaterRepository(private val context: Context) {

    val countFlow: Flow<Int> = context.waterDataStore.data
        .map { prefs -> prefs[WaterKeys.REMINDER_COUNT] ?: 0 }

    val enabledFlow: Flow<Boolean> = context.waterDataStore.data
        .map { prefs -> prefs[WaterKeys.ENABLED] ?: false }

    val intervalFlow: Flow<Long> = context.waterDataStore.data
        .map { prefs -> prefs[WaterKeys.INTERVAL] ?: 15L }

    suspend fun incrementCount() {
        context.waterDataStore.edit { prefs ->
            prefs[WaterKeys.REMINDER_COUNT] = (prefs[WaterKeys.REMINDER_COUNT] ?: 0) + 1
        }
    }

    suspend fun saveSettings(enabled: Boolean, interval: Long) {
        context.waterDataStore.edit { prefs ->
            prefs[WaterKeys.ENABLED] = enabled
            prefs[WaterKeys.INTERVAL] = interval
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.waterDataStore.edit { prefs -> prefs[WaterKeys.ENABLED] = enabled }
    }
}