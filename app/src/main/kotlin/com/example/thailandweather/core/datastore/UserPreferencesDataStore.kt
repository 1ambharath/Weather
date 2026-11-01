package com.example.thailandweather.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.thailandweather.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val TEMP_UNIT_CELSIUS = booleanPreferencesKey("temp_unit_celsius")
        val WIND_UNIT_KMH = booleanPreferencesKey("wind_unit_kmh")
        val LANGUAGE_THAI = booleanPreferencesKey("language_thai")
        val PROXIMITY_RADIUS_KM = intPreferencesKey("proximity_radius_km")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_OFFICIAL_WARNINGS = booleanPreferencesKey("notify_official_warnings")
        val NOTIFY_FORECAST_RISKS = booleanPreferencesKey("notify_forecast_risks")
        val NOTIFY_AIR_QUALITY = booleanPreferencesKey("notify_air_quality")
        val REFRESH_INTERVAL_MINUTES = intPreferencesKey("refresh_interval_minutes")
        val AI_SELECTED_MODEL = stringPreferencesKey("ai_selected_model")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val selectedAiModelFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val saved = preferences[Keys.AI_SELECTED_MODEL]
            if (saved == null || saved == "mixtral-8x7b-32768") "llama-3.3-70b-versatile" else saved
        }

    suspend fun setAiSelectedModel(model: String) {
        context.dataStore.edit { it[Keys.AI_SELECTED_MODEL] = model }
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                tempUnitCelsius = prefs[Keys.TEMP_UNIT_CELSIUS] ?: true,
                windUnitKmh = prefs[Keys.WIND_UNIT_KMH] ?: true,
                languageThai = prefs[Keys.LANGUAGE_THAI] ?: false,
                proximityRadiusKm = prefs[Keys.PROXIMITY_RADIUS_KM] ?: 25,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                notifyOfficialWarnings = prefs[Keys.NOTIFY_OFFICIAL_WARNINGS] ?: true,
                notifyForecastRisks = prefs[Keys.NOTIFY_FORECAST_RISKS] ?: true,
                notifyAirQuality = prefs[Keys.NOTIFY_AIR_QUALITY] ?: true,
                autoRefreshIntervalMinutes = prefs[Keys.REFRESH_INTERVAL_MINUTES] ?: 30,
                darkMode = prefs[Keys.DARK_MODE] ?: false
            )
        }

    suspend fun setTempUnitCelsius(isCelsius: Boolean) {
        context.dataStore.edit { it[Keys.TEMP_UNIT_CELSIUS] = isCelsius }
    }

    suspend fun setWindUnitKmh(isKmh: Boolean) {
        context.dataStore.edit { it[Keys.WIND_UNIT_KMH] = isKmh }
    }

    suspend fun setLanguageThai(isThai: Boolean) {
        context.dataStore.edit { it[Keys.LANGUAGE_THAI] = isThai }
    }

    suspend fun setProximityRadiusKm(radiusKm: Int) {
        context.dataStore.edit { it[Keys.PROXIMITY_RADIUS_KM] = radiusKm }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotifyOfficialWarnings(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_OFFICIAL_WARNINGS] = enabled }
    }

    suspend fun setNotifyForecastRisks(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_FORECAST_RISKS] = enabled }
    }

    suspend fun setNotifyAirQuality(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_AIR_QUALITY] = enabled }
    }

    suspend fun setAutoRefreshIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REFRESH_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }
}
