package com.example.thailandweather.ui.briefing

import com.example.thailandweather.core.model.AiBriefing
import com.example.thailandweather.core.model.ConnectionStatus
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
class AiBriefingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeBriefingRepository(
        var keyConfigured: Boolean = false,
        var briefingToEmit: AiBriefing? = null
    ) : AiRepository {
        val briefingFlow = MutableStateFlow<AiBriefing?>(briefingToEmit)

        override val selectedModelFlow: Flow<String> = MutableStateFlow("llama-3.3-70b-versatile")
        override suspend fun setSelectedModel(model: String) {}
        override suspend fun hasConfiguredKey(): Boolean = keyConfigured
        override suspend fun getMaskedKeyDisplay(): String? = if (keyConfigured) "gsk_••••••••1234" else null
        override suspend fun saveApiKey(key: String) { keyConfigured = true }
        override suspend fun removeApiKey() { keyConfigured = false; briefingFlow.value = null }
        override suspend fun testConnection(): Result<ConnectionStatus> =
            Result.success(if (keyConfigured) ConnectionStatus.CONNECTED else ConnectionStatus.NOT_CONFIGURED)
        override suspend fun getAvailableModels(): Result<List<String>> =
            Result.success(listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"))
        override val latestBriefingFlow: Flow<AiBriefing?> = briefingFlow
        override suspend fun getLatestBriefing(): AiBriefing? = briefingFlow.value
        var lastRequestedPlaceId: String? = null

        override suspend fun generateBriefing(selectedPlaceId: String?): Result<AiBriefing> {
            lastRequestedPlaceId = selectedPlaceId
            if (!keyConfigured) return Result.failure(Exception("Groq API key is not configured"))
            val generated = AiBriefing(
                title = "Generated Thailand Intelligence Briefing",
                thailandSummary = "Seasonal monsoon showers tapering off in the north.",
                selectedAreaBriefing = com.example.thailandweather.core.model.AiSelectedAreaBriefing(
                    placeName = "Bangkok",
                    province = "Bangkok",
                    summary = "Scattered light showers during evening commute.",
                    conditionsHighlight = "33°C / Feels 39°C • 20% Rain • 12 km/h Wind • UV 7 High",
                    riskLevel = "Low",
                    travelRecommendation = "Ideal for morning errands; carry light umbrella."
                ),
                newsAndIncidents = "TMD confirms typhoon threat averted.",
                sources = listOf("TMD", "Open-Meteo"),
                isCached = true
            )
            briefingFlow.value = generated
            return Result.success(generated)
        }
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
    fun testGatingWhenNoKeyConfigured() = runTest {
        val repository = FakeBriefingRepository(keyConfigured = false)
        val viewModel = AiBriefingViewModel(repository)

        testScheduler.advanceUntilIdle()

        assertFalse("AI feature must be gated when no key exists", viewModel.uiState.value.hasKey)
        assertNull(viewModel.uiState.value.briefing)
    }

    @Test
    fun testOfflineAccessToCachedBriefing() = runTest {
        val cachedBriefing = AiBriefing(
            title = "Cached Offline Briefing",
            thailandSummary = "Clear skies over Bangkok.",
            newsAndIncidents = "No active warnings.",
            sources = listOf("TMD"),
            isCached = true,
            isStale = true
        )
        val repository = FakeBriefingRepository(keyConfigured = true, briefingToEmit = cachedBriefing)
        val viewModel = AiBriefingViewModel(repository)

        testScheduler.advanceUntilIdle()

        assertTrue("Key should be recognized as configured", viewModel.uiState.value.hasKey)
        assertNotNull("Cached briefing must be accessible offline", viewModel.uiState.value.briefing)
        assertEquals("Cached Offline Briefing", viewModel.uiState.value.briefing!!.title)
        assertTrue("Briefing should be marked as cached", viewModel.uiState.value.briefing!!.isCached)
        assertTrue("Briefing should indicate potentially stale cache status", viewModel.uiState.value.briefing!!.isStale)
    }

    @Test
    fun testGenerateBriefingWithoutKeyFailsGracefully() = runTest {
        val repository = FakeBriefingRepository(keyConfigured = false)
        val viewModel = AiBriefingViewModel(repository)

        testScheduler.advanceUntilIdle()
        viewModel.generateBriefing()
        testScheduler.advanceUntilIdle()

        assertNotNull("Error message should be set when attempting to generate without key", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("required"))
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun testGenerateBriefingWithKeySucceeds() = runTest {
        val repository = FakeBriefingRepository(keyConfigured = true)
        val viewModel = AiBriefingViewModel(repository)

        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.briefing)

        viewModel.generateBriefing()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertNotNull(viewModel.uiState.value.briefing)
        assertEquals("Generated Thailand Intelligence Briefing", viewModel.uiState.value.briefing!!.title)
    }

    @Test
    fun testGenerateBriefingIncludesSelectedAreaBriefing() = runTest {
        val repository = FakeBriefingRepository(keyConfigured = true)
        val viewModel = AiBriefingViewModel(repository)

        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.briefing)

        viewModel.generateBriefing()
        testScheduler.advanceUntilIdle()

        val briefing = viewModel.uiState.value.briefing
        assertNotNull(briefing)
        assertNotNull(briefing!!.selectedAreaBriefing)
        assertEquals("Bangkok", briefing.selectedAreaBriefing.placeName)
        assertEquals("Low", briefing.selectedAreaBriefing.riskLevel)
        assertTrue(briefing.selectedAreaBriefing!!.conditionsHighlight.contains("33°C"))
    }
}
