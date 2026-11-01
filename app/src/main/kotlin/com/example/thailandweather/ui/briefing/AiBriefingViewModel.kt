package com.example.thailandweather.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.AiBriefing
import com.example.thailandweather.data.repository.AiRepository
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
    val error: String? = null
)

class AiBriefingViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiBriefingUiState())
    val uiState: StateFlow<AiBriefingUiState> = _uiState.asStateFlow()

    init {
        checkKeyAndLoadCachedBriefing()
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

    fun generateBriefing() {
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

            val result = aiRepository.generateBriefing()
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

    class Factory(private val aiRepository: AiRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiBriefingViewModel(aiRepository) as T
        }
    }
}
