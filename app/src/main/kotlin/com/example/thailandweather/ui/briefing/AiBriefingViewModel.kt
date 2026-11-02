package com.example.thailandweather.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.AiBriefing
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.data.repository.AiRepository
import com.example.thailandweather.data.repository.PlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiBriefingUiState(
    val hasKey: Boolean = false,
    val briefing: AiBriefing? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPlace: Place? = null,
    val savedPlaces: List<Place> = emptyList()
)

class AiBriefingViewModel(
    private val aiRepository: AiRepository,
    private val placesRepository: PlacesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiBriefingUiState())
    val uiState: StateFlow<AiBriefingUiState> = _uiState.asStateFlow()

    init {
        checkKeyAndLoadCachedBriefing()
        loadPlaces()
    }

    fun checkKeyAndLoadCachedBriefing() {
        viewModelScope.launch {
            val hasKey = aiRepository.hasConfiguredKey()
            _uiState.update { it.copy(hasKey = hasKey) }

            // Observe cached Room briefings so offline reading is instant
            aiRepository.latestBriefingFlow.collectLatest { cached ->
                _uiState.update { it.copy(briefing = cached) }
            }
        }
    }

    private fun loadPlaces() {
        if (placesRepository == null) return
        viewModelScope.launch {
            placesRepository.allPlaces.collectLatest { places ->
                _uiState.update { current ->
                    // Validate that current selectedPlace still exists in saved places;
                    // if it was deleted, immediately reset to default or first remaining place
                    val active = if (places.any { it.id == current.selectedPlace?.id }) {
                        current.selectedPlace
                    } else {
                        places.firstOrNull { it.isDefault } ?: places.firstOrNull()
                    }
                    current.copy(
                        savedPlaces = places,
                        selectedPlace = active
                    )
                }
            }
        }
    }

    fun selectPlace(place: Place) {
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun generateBriefing(placeId: String? = null) {
        viewModelScope.launch {
            val hasKey = aiRepository.hasConfiguredKey()
            if (!hasKey) {
                _uiState.update {
                    it.copy(
                        hasKey = false,
                        error = "A Groq API key is required to generate briefings. Configure your key in AI Settings."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            // Ensure we never pass an orphaned or deleted place id
            val currentPlaces = _uiState.value.savedPlaces
            val targetId = if (placeId != null && currentPlaces.any { it.id == placeId }) {
                placeId
            } else {
                _uiState.value.selectedPlace?.id ?: currentPlaces.firstOrNull { it.isDefault }?.id ?: currentPlaces.firstOrNull()?.id
            }

            val result = aiRepository.generateBriefing(targetId)
            result.fold(
                onSuccess = { newBriefing ->
                    _uiState.update {
                        it.copy(
                            briefing = newBriefing,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Failed to generate briefing"
                        )
                    }
                }
            )
        }
    }

    class Factory(
        private val aiRepository: AiRepository,
        private val placesRepository: PlacesRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiBriefingViewModel(aiRepository, placesRepository) as T
        }
    }
}
