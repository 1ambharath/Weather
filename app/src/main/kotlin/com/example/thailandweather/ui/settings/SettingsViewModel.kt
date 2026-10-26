package com.example.thailandweather.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.datastore.UserPreferencesDataStore
import com.example.thailandweather.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun toggleTempUnit(isCelsius: Boolean) {
        viewModelScope.launch { preferencesDataStore.setTempUnitCelsius(isCelsius) }
    }

    fun toggleWindUnit(isKmh: Boolean) {
        viewModelScope.launch { preferencesDataStore.setWindUnitKmh(isKmh) }
    }

    fun toggleLanguage(isThai: Boolean) {
        viewModelScope.launch { preferencesDataStore.setLanguageThai(isThai) }
    }

    fun setProximityRadius(radiusKm: Int) {
        viewModelScope.launch { preferencesDataStore.setProximityRadiusKm(radiusKm) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setNotificationsEnabled(enabled) }
    }

    fun toggleOfficialWarnings(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setNotifyOfficialWarnings(enabled) }
    }

    fun toggleForecastRisks(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setNotifyForecastRisks(enabled) }
    }

    fun toggleAirQuality(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.setNotifyAirQuality(enabled) }
    }

    fun setRefreshInterval(minutes: Int) {
        viewModelScope.launch { preferencesDataStore.setAutoRefreshIntervalMinutes(minutes) }
    }

    class Factory(private val dataStore: UserPreferencesDataStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(dataStore) as T
        }
    }
}
