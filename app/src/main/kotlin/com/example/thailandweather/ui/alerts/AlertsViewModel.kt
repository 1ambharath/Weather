package com.example.thailandweather.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.ForecastRisk
import com.example.thailandweather.core.model.OfficialAlert
import com.example.thailandweather.data.repository.AlertsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AlertsUiState(
    val officialAlerts: List<OfficialAlert> = emptyList(),
    val forecastRisks: List<ForecastRisk> = emptyList(),
    val selectedOfficialAlert: OfficialAlert? = null,
    val selectedForecastRisk: ForecastRisk? = null,
    val isRefreshing: Boolean = false
)

class AlertsViewModel(
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState(isRefreshing = true))
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        observeAlerts()
        refreshAlerts()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            combine(
                alertsRepository.activeOfficialAlerts,
                alertsRepository.activeForecastRisks
            ) { official, risks ->
                _uiState.update {
                    it.copy(
                        officialAlerts = official,
                        forecastRisks = risks,
                        isRefreshing = false
                    )
                }
            }.collect()
        }
    }

    fun refreshAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            alertsRepository.refreshOfficialAlerts()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun selectOfficialAlert(alert: OfficialAlert?) {
        _uiState.update { it.copy(selectedOfficialAlert = alert) }
    }

    fun selectForecastRisk(risk: ForecastRisk?) {
        _uiState.update { it.copy(selectedForecastRisk = risk) }
    }

    class Factory(private val alertsRepo: AlertsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlertsViewModel(alertsRepo) as T
        }
    }
}
