package com.example.thailandweather.ui.settings

import com.example.thailandweather.core.model.AiBriefing
import com.example.thailandweather.core.model.ConnectionStatus
import com.example.thailandweather.core.security.KeystoreEncryptedKeyStorage
import com.example.thailandweather.data.remote.GroqException
import com.example.thailandweather.data.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAiRepository(
        var configuredKey: String? = null,
        override val selectedModelFlow: Flow<String> = MutableStateFlow("llama-3.3-70b-versatile"),
        override val latestBriefingFlow: Flow<AiBriefing?> = MutableStateFlow(null)
    ) : AiRepository {
        override suspend fun setSelectedModel(model: String) {}
        override suspend fun hasConfiguredKey(): Boolean = !configuredKey.isNullOrBlank()
        override suspend fun getMaskedKeyDisplay(): String? =
            configuredKey?.let { KeystoreEncryptedKeyStorage.createMaskedRepresentation(it) }

        override suspend fun saveApiKey(key: String) {
            configuredKey = key.trim()
        }

        override suspend fun removeApiKey() {
            configuredKey = null
        }

        override suspend fun testConnection(): Result<ConnectionStatus> {
            val k = configuredKey ?: return Result.success(ConnectionStatus.NOT_CONFIGURED)
            return if (k.contains("invalid")) {
                Result.failure(GroqException.InvalidKey())
            } else if (k.contains("ratelimit")) {
                Result.failure(GroqException.RateLimitExceeded())
            } else {
                Result.success(ConnectionStatus.CONNECTED)
            }
        }

        override suspend fun getLatestBriefing(): AiBriefing? = null
        override suspend fun getAvailableModels(): Result<List<String>> =
            Result.success(listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"))
        override suspend fun generateBriefing(): Result<AiBriefing> =
            Result.success(AiBriefing(title = "Test", thailandSummary = "Test"))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateWhenNoKeyConfigured() = runTest {
        val fakeRepository = FakeAiRepository(configuredKey = null)
        val viewModel = AiSettingsViewModel(fakeRepository)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should not have key initially", state.hasKey)
        assertNull("Masked display should be null", state.maskedKeyDisplay)
        assertEquals(ConnectionStatus.NOT_CONFIGURED, state.connectionStatus)
    }

    @Test
    fun testKeyVisibilityToggle() = runTest {
        val fakeRepository = FakeAiRepository()
        val viewModel = AiSettingsViewModel(fakeRepository)

        assertFalse("Key should be masked by default", viewModel.uiState.value.isKeyVisible)

        viewModel.toggleKeyVisibility()
        assertTrue("Key should be unmasked after toggle", viewModel.uiState.value.isKeyVisible)

        viewModel.toggleKeyVisibility()
        assertFalse("Key should be masked again", viewModel.uiState.value.isKeyVisible)
    }

    @Test
    fun testSaveKeyUpdatesStateAndClearsInputBuffer() = runTest {
        val fakeRepository = FakeAiRepository()
        val viewModel = AiSettingsViewModel(fakeRepository)

        testScheduler.advanceUntilIdle()

        viewModel.onKeyInputChanged("gsk_my_secret_token_12345")
        assertEquals("gsk_my_secret_token_12345", viewModel.uiState.value.enteredKey)

        viewModel.saveKey()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("hasKey should be true", state.hasKey)
        assertEquals("Entered key should be cleared from memory", "", state.enteredKey)
        assertNotNull("Masked key display should be populated", state.maskedKeyDisplay)
        assertTrue(state.maskedKeyDisplay!!.startsWith("gsk_"))
        assertEquals(ConnectionStatus.KEY_SAVED, state.connectionStatus)
    }

    @Test
    fun testRemoveKeyResetsState() = runTest {
        val fakeRepository = FakeAiRepository(configuredKey = "gsk_existing_key_98765")
        val viewModel = AiSettingsViewModel(fakeRepository)

        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasKey)

        viewModel.removeKey()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasKey)
        assertNull(state.maskedKeyDisplay)
        assertEquals(ConnectionStatus.NOT_CONFIGURED, state.connectionStatus)
    }

    @Test
    fun testConnectionTestSuccessful() = runTest {
        val fakeRepository = FakeAiRepository(configuredKey = "gsk_valid_key_1234")
        val viewModel = AiSettingsViewModel(fakeRepository)

        testScheduler.advanceUntilIdle()
        assertEquals(ConnectionStatus.KEY_SAVED, viewModel.uiState.value.connectionStatus)

        viewModel.testConnection()
        testScheduler.advanceUntilIdle()

        assertEquals(ConnectionStatus.CONNECTED, viewModel.uiState.value.connectionStatus)
        assertFalse(viewModel.uiState.value.isTesting)
    }

    @Test
    fun testConnectionTestInvalidKey() = runTest {
        val fakeRepository = FakeAiRepository(configuredKey = "gsk_invalid_key_9999")
        val viewModel = AiSettingsViewModel(fakeRepository)

        testScheduler.advanceUntilIdle()
        viewModel.testConnection()
        testScheduler.advanceUntilIdle()

        assertEquals(ConnectionStatus.INVALID_KEY, viewModel.uiState.value.connectionStatus)
        assertFalse(viewModel.uiState.value.isTesting)
    }

    @Test
    fun testConnectionTestRateLimit() = runTest {
        val fakeRepository = FakeAiRepository(configuredKey = "gsk_ratelimit_key_7777")
        val viewModel = AiSettingsViewModel(fakeRepository)

        testScheduler.advanceUntilIdle()
        viewModel.testConnection()
        testScheduler.advanceUntilIdle()

        assertEquals(ConnectionStatus.RATE_LIMIT_EXCEEDED, viewModel.uiState.value.connectionStatus)
        assertFalse(viewModel.uiState.value.isTesting)
    }
}
