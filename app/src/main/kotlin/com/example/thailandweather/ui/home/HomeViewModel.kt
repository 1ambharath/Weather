package com.example.thailandweather.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.*
import com.example.thailandweather.data.repository.AlertsRepository
import com.example.thailandweather.data.repository.NewsRepository
import com.example.thailandweather.data.repository.PlacesRepository
import com.example.thailandweather.data.repository.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedPlace: Place? = null,
    val allPlaces: List<Place> = emptyList(),
    val currentSnapshot: WeatherSnapshot? = null,
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val highPriorityAlerts: List<OfficialAlert> = emptyList(),
    val recentNews: List<Article> = emptyList(),
    val comparisonSnapshots: Map<String, WeatherSnapshot> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val aiBriefingSummary: String? = null
)

class HomeViewModel(
    private val placesRepository: PlacesRepository,
    private val weatherRepository: WeatherRepository,
    private val alertsRepository: AlertsRepository,
    private val newsRepository: NewsRepository,
    private val aiRepository: com.example.thailandweather.data.repository.AiRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        observeData()
        observeAiBriefing()
    }

    private fun observeAiBriefing() {
        val repo = aiRepository ?: return
        viewModelScope.launch {
            repo.latestBriefingFlow.collect { briefing ->
                _uiState.update { it.copy(aiBriefingSummary = briefing?.thailandSummary) }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                placesRepository.allPlaces,
                placesRepository.defaultPlace,
                alertsRepository.activeOfficialAlerts,
                newsRepository.allArticles
            ) { places, defaultPlace, alerts, news ->
                val currentPlace = _uiState.value.selectedPlace ?: defaultPlace ?: places.firstOrNull()
                _uiState.update { state ->
                    state.copy(
                        selectedPlace = currentPlace,
                        allPlaces = places,
                        highPriorityAlerts = alerts.filter { it.severity.level >= AlertSeverity.WATCH.level },
                        recentNews = news.take(4)
                    )
                }
                currentPlace?.let { place ->
                    loadWeatherForPlace(place, forceRefresh = false)
                }
                // Load batch for comparison
                loadComparisonSnapshots(places)
            }.collect()
        }
    }

    fun selectPlace(place: Place) {
        _uiState.update { it.copy(selectedPlace = place) }
        loadWeatherForPlace(place, forceRefresh = false)
    }

    fun refresh() {
        val place = _uiState.value.selectedPlace ?: return
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val startTime = System.currentTimeMillis()

            try {
                val refreshResult = weatherRepository.refreshWeather(place)
                if (refreshResult.isSuccess && refreshResult.snapshot != null) {
                    _uiState.update { state ->
                        state.copy(
                            currentSnapshot = refreshResult.snapshot,
                            hourlyForecast = refreshResult.hourly,
                            dailyForecast = refreshResult.daily,
                            isLoading = false
                        )
                    }
                    alertsRepository.evaluateForecastRisks(place, refreshResult.snapshot)
                }

                alertsRepository.refreshOfficialAlerts()
                newsRepository.refreshArticles()
                loadComparisonSnapshots(_uiState.value.allPlaces)

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 600) {
                    delay(600 - elapsed)
                }

                val message = if (refreshResult.hasNewData) "loaded fresh contents" else "No fresh updates"
                _toastMessage.emit(message)
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 600) {
                    delay(600 - elapsed)
                }
                _toastMessage.emit("No fresh updates")
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun loadWeatherForPlace(place: Place, forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.currentSnapshot == null) }
            weatherRepository.getWeatherStream(place, forceRefresh = forceRefresh).collect { snapshot ->
                val hourly = weatherRepository.getHourlyForecast(place)
                val daily = weatherRepository.getDailyForecast(place)
                _uiState.update { state ->
                    state.copy(
                        currentSnapshot = snapshot,
                        hourlyForecast = hourly,
                        dailyForecast = daily,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                // Also evaluate forecast risks
                alertsRepository.evaluateForecastRisks(place, snapshot)
            }
        }
    }

    private fun loadComparisonSnapshots(places: List<Place>) {
        if (places.isEmpty()) return
        viewModelScope.launch {
            val batch = weatherRepository.getBatchWeatherSnapshots(places)
            _uiState.update { it.copy(comparisonSnapshots = batch) }
        }
    }

    class Factory(
        private val placesRepo: PlacesRepository,
        private val weatherRepo: WeatherRepository,
        private val alertsRepo: AlertsRepository,
        private val newsRepo: NewsRepository,
        private val aiRepo: com.example.thailandweather.data.repository.AiRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(placesRepo, weatherRepo, alertsRepo, newsRepo, aiRepo) as T
        }
    }
}
