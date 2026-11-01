package com.example.thailandweather.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thailandweather.core.model.ConnectionStatus
import com.example.thailandweather.core.model.GroqModelInfo
import com.example.thailandweather.data.remote.GroqException
import com.example.thailandweather.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiSettingsUiState(
    val hasKey: Boolean = false,
    val maskedKeyDisplay: String? = null,
    val enteredKey: String = "",
    val isKeyVisible: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONFIGURED,
    val isTesting: Boolean = false,
    val statusFeedback: String? = null,
    val selectedModel: String = GroqModelInfo.DEFAULT_MODEL.id,
    val supportedModels: List<GroqModelInfo> = GroqModelInfo.SUPPORTED_MODELS
)

class AiSettingsViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        observeSelectedModel()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val hasKey = aiRepository.hasConfiguredKey()
            val masked = aiRepository.getMaskedKeyDisplay()
            val status = if (hasKey) ConnectionStatus.KEY_SAVED else ConnectionStatus.NOT_CONFIGURED

            _uiState.update {
                it.copy(
                    hasKey = hasKey,
                    maskedKeyDisplay = masked,
                    connectionStatus = status,
                    statusFeedback = if (hasKey) "API key is saved in hardware-backed encrypted storage." else null
                )
            }

            if (hasKey) {
                refreshAvailableModels()
            }
        }
    }

    private suspend fun refreshAvailableModels() {
        val modelsResult = aiRepository.getAvailableModels()
        modelsResult.onSuccess { activeIds ->
            if (activeIds.isNotEmpty()) {
                val updatedSupported = GroqModelInfo.SUPPORTED_MODELS.map { modelInfo ->
                    val isAvailable = activeIds.any { it.equals(modelInfo.id, ignoreCase = true) }
                    modelInfo.copy(
                        description = if (isAvailable) modelInfo.description else "${modelInfo.description} (Not enabled for this account)"
                    )
                }

                val current = _uiState.value.selectedModel
                val isCurrentAvailable = activeIds.any { it.equals(current, ignoreCase = true) }

                // Find best matching model enabled on this Groq account
                val bestAvailable = GroqModelInfo.SUPPORTED_MODELS.firstOrNull { supported ->
                    activeIds.any { it.equals(supported.id, ignoreCase = true) }
                }?.id ?: activeIds.firstOrNull {
                    !it.contains("whisper", ignoreCase = true) &&
                    !it.contains("guard", ignoreCase = true)
                } ?: activeIds.first()

                val resolvedModel = if (isCurrentAvailable) current else bestAvailable

                _uiState.update { state ->
                    state.copy(
                        supportedModels = updatedSupported,
                        selectedModel = resolvedModel
                    )
                }

                if (!isCurrentAvailable) {
                    selectModel(resolvedModel)
                }
            }
        }
    }

    private fun observeSelectedModel() {
        viewModelScope.launch {
            aiRepository.selectedModelFlow.collectLatest { modelId ->
                _uiState.update { it.copy(selectedModel = modelId) }
            }
        }
    }

    fun onKeyInputChanged(input: String) {
        _uiState.update { it.copy(enteredKey = input) }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun saveKey() {
        val key = _uiState.value.enteredKey.trim()
        if (key.isBlank()) return

        viewModelScope.launch {
            aiRepository.saveApiKey(key)
            val masked = aiRepository.getMaskedKeyDisplay()
            _uiState.update {
                it.copy(
                    hasKey = true,
                    maskedKeyDisplay = masked,
                    enteredKey = "", // Clear input buffer from memory
                    connectionStatus = ConnectionStatus.KEY_SAVED,
                    statusFeedback = "Key securely encrypted. Tap 'Test Connection' to verify with Groq."
                )
            }
            refreshAvailableModels()
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            aiRepository.removeApiKey()
            _uiState.update {
                it.copy(
                    hasKey = false,
                    maskedKeyDisplay = null,
                    enteredKey = "",
                    connectionStatus = ConnectionStatus.NOT_CONFIGURED,
                    statusFeedback = "API key and cached briefings removed."
                )
            }
        }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            aiRepository.setSelectedModel(modelId)
            _uiState.update { it.copy(selectedModel = modelId) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTesting = true,
                    connectionStatus = ConnectionStatus.CHECKING_CONNECTION,
                    statusFeedback = "Connecting to Groq API (${it.selectedModel})..."
                )
            }

            val result = aiRepository.testConnection()
            result.fold(
                onSuccess = { status ->
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            connectionStatus = status,
                            statusFeedback = if (status == ConnectionStatus.CONNECTED) {
                                "Connected successfully! Ready to generate briefings."
                            } else {
                                "Please save an API key first."
                            }
                        )
                    }
                    refreshAvailableModels()
                },
                onFailure = { error ->
                    val status = when (error) {
                        is GroqException.InvalidKey -> ConnectionStatus.INVALID_KEY
                        is GroqException.RateLimitExceeded -> ConnectionStatus.RATE_LIMIT_EXCEEDED
                        else -> ConnectionStatus.CONNECTION_FAILED
                    }
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            connectionStatus = status,
                            statusFeedback = error.localizedMessage ?: "Failed to connect to Groq"
                        )
                    }
                }
            )
        }
    }

    class Factory(private val aiRepository: AiRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiSettingsViewModel(aiRepository) as T
        }
    }
}
