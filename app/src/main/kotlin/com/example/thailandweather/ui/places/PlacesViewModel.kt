package com.example.thailandweather.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.model.WeatherSnapshot
import com.example.thailandweather.core.util.Haversine
import com.example.thailandweather.data.provider.WeatherProvider
import com.example.thailandweather.data.repository.PlacesRepository
import com.example.thailandweather.data.repository.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class PlacesViewMode {
    COMPARISON_LIST,
    PROXIMITY_CLUSTERS,
    CUSTOM_GROUPS
}

data class PlacesUiState(
    val places: List<Place> = emptyList(),
    val clusters: List<Haversine.PlaceCluster> = emptyList(),
    val customGroupsMap: Map<String, List<Place>> = emptyMap(),
    val snapshots: Map<String, WeatherSnapshot> = emptyMap(),
    val viewMode: PlacesViewMode = PlacesViewMode.COMPARISON_LIST,
    val selectedRadiusKm: Double = 25.0,
    val searchQuery: String = "",
    val searchResults: List<Place> = emptyList(),
    val isSearching: Boolean = false,
    val isAddingPlace: Boolean = false,
    val isLoading: Boolean = false
)

class PlacesViewModel(
    private val placesRepository: PlacesRepository,
    private val weatherRepository: WeatherRepository,
    private val weatherProvider: WeatherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacesUiState())
    val uiState: StateFlow<PlacesUiState> = _uiState.asStateFlow()

    init {
        observePlaces()
    }

    private fun observePlaces() {
        viewModelScope.launch {
            placesRepository.allPlaces.collect { places ->
                val groups = places.groupBy { it.customGroup ?: "General" }
                val clusters = Haversine.clusterPlaces(places, _uiState.value.selectedRadiusKm)
                _uiState.update { state ->
                    state.copy(
                        places = places,
                        customGroupsMap = groups,
                        clusters = clusters
                    )
                }
                loadWeatherSnapshots(places)
            }
        }
    }

    private fun loadWeatherSnapshots(places: List<Place>) {
        if (places.isEmpty()) return
        viewModelScope.launch {
            val batch = weatherRepository.getBatchWeatherSnapshots(places)
            _uiState.update { it.copy(snapshots = batch) }
        }
    }

    fun setViewMode(mode: PlacesViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setProximityRadius(radiusKm: Double) {
        val clusters = Haversine.clusterPlaces(_uiState.value.places, radiusKm)
        _uiState.update { it.copy(selectedRadiusKm = radiusKm, clusters = clusters) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length >= 2) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSearching = true) }
                val results = weatherProvider.searchLocations(query)
                _uiState.update {
                    it.copy(
                        searchResults = results.getOrDefault(emptyList()),
                        isSearching = false
                    )
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun addPlace(place: Place) {
        viewModelScope.launch {
            placesRepository.addPlace(place)
            _uiState.update { it.copy(isAddingPlace = false, searchQuery = "", searchResults = emptyList()) }
        }
    }

    fun addCustomCoordinatePlace(name: String, province: String, lat: Double, lon: Double, group: String?) {
        val newPlace = Place(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = name,
            province = province,
            latitude = lat,
            longitude = lon,
            customGroup = group ?: "Custom Places",
            isDefault = false
        )
        addPlace(newPlace)
    }

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            placesRepository.deletePlace(placeId)
        }
    }

    fun setDefault(placeId: String) {
        viewModelScope.launch {
            placesRepository.setDefaultPlace(placeId)
        }
    }

    fun toggleAlerts(place: Place) {
        viewModelScope.launch {
            placesRepository.updatePlace(place.copy(alertEnabled = !place.alertEnabled))
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(isAddingPlace = show) }
    }

    class Factory(
        private val placesRepo: PlacesRepository,
        private val weatherRepo: WeatherRepository,
        private val provider: WeatherProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlacesViewModel(placesRepo, weatherRepo, provider) as T
        }
    }
}
