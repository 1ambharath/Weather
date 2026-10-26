package com.example.thailandweather.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.model.WeatherSnapshot
import com.example.thailandweather.data.remote.SatelliteProduct
import com.example.thailandweather.data.remote.TmdRadarCatalog
import com.example.thailandweather.data.remote.TmdRadarStation
import com.example.thailandweather.data.repository.PlacesRepository
import com.example.thailandweather.data.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MapOverlayType(val label: String) {
    RADAR("TMD Radar"),
    PRECIPITATION("Rainfall"),
    TEMPERATURE("Temperature"),
    WIND("Wind Flow"),
    SATELLITE("Himawari Satellite")
}

data class ExploreUiState(
    val places: List<Place> = emptyList(),
    val selectedOverlay: MapOverlayType = MapOverlayType.RADAR,
    val radarStations: List<TmdRadarStation> = TmdRadarCatalog.STATIONS,
    val selectedStation: TmdRadarStation = TmdRadarCatalog.STATIONS.first(),
    val satelliteProducts: List<SatelliteProduct> = TmdRadarCatalog.SATELLITE_PRODUCTS,
    val selectedSatellite: SatelliteProduct = TmdRadarCatalog.SATELLITE_PRODUCTS.first(),
    val inspectedPlace: Place? = null,
    val inspectedWeather: WeatherSnapshot? = null,
    val isPlayingLoop: Boolean = false,
    val loopFrameIndex: Int = 4, // 0 to 4 (e.g. -60m to now)
    val isLoading: Boolean = false
)

class ExploreViewModel(
    private val placesRepository: PlacesRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            placesRepository.allPlaces.collect { list ->
                _uiState.update { it.copy(places = list) }
            }
        }
    }

    fun selectOverlay(type: MapOverlayType) {
        _uiState.update { it.copy(selectedOverlay = type) }
    }

    fun selectStation(station: TmdRadarStation) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun selectSatellite(product: SatelliteProduct) {
        _uiState.update { it.copy(selectedSatellite = product) }
    }

    fun toggleLoopPlayback() {
        _uiState.update { it.copy(isPlayingLoop = !it.isPlayingLoop) }
    }

    fun setLoopFrame(index: Int) {
        _uiState.update { it.copy(loopFrameIndex = index) }
    }

    fun inspectPlace(place: Place) {
        _uiState.update { it.copy(inspectedPlace = place, isLoading = true) }
        viewModelScope.launch {
            weatherRepository.getWeatherStream(place, forceRefresh = false).collect { snap ->
                _uiState.update { it.copy(inspectedWeather = snap, isLoading = false) }
            }
        }
    }

    fun clearInspection() {
        _uiState.update { it.copy(inspectedPlace = null, inspectedWeather = null) }
    }

    class Factory(
        private val placesRepo: PlacesRepository,
        private val weatherRepo: WeatherRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExploreViewModel(placesRepo, weatherRepo) as T
        }
    }
}
