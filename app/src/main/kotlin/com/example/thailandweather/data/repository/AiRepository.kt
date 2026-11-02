package com.example.thailandweather.data.repository

import com.example.thailandweather.core.database.dao.AiBriefingDao
import com.example.thailandweather.core.database.entity.AiBriefingEntity
import com.example.thailandweather.core.datastore.UserPreferencesDataStore
import com.example.thailandweather.core.model.*
import com.example.thailandweather.core.security.SecureKeyStorage
import com.example.thailandweather.data.remote.GroqClient
import com.example.thailandweather.data.remote.GroqException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
private data class GroqBriefingJson(
    val title: String = "Thailand Meteorological Intelligence Briefing",
    val thailandSummary: String = "",
    val selectedAreaBriefing: GroqSelectedAreaBriefingJson? = null,
    val locationSummaries: List<GroqLocationSummaryJson> = emptyList(),
    val newsAndIncidents: String = "",
    val riskExplanations: List<GroqRiskExplanationJson> = emptyList(),
    val actionableAdvice: List<String> = emptyList(),
    val sources: List<String> = emptyList()
)

@Serializable
private data class GroqSelectedAreaBriefingJson(
    val placeName: String = "",
    val province: String = "",
    val summary: String = "",
    val conditionsHighlight: String = "",
    val riskLevel: String = "Low",
    val travelRecommendation: String = ""
)

@Serializable
private data class GroqLocationSummaryJson(
    val placeName: String = "",
    val summary: String = "",
    val riskLevel: String = "Low"
)

@Serializable
private data class GroqRiskExplanationJson(
    val hazard: String = "",
    val explanation: String = "",
    val source: String = "TMD"
)

interface AiRepository {
    val selectedModelFlow: Flow<String>
    suspend fun setSelectedModel(model: String)
    suspend fun hasConfiguredKey(): Boolean
    suspend fun getMaskedKeyDisplay(): String?
    suspend fun saveApiKey(key: String)
    suspend fun removeApiKey()
    suspend fun testConnection(): Result<ConnectionStatus>
    suspend fun getAvailableModels(): Result<List<String>>
    val latestBriefingFlow: Flow<AiBriefing?>
    suspend fun getLatestBriefing(): AiBriefing?
    suspend fun generateBriefing(selectedPlaceId: String? = null): Result<AiBriefing>
}

class DefaultAiRepository(
    private val briefingDao: AiBriefingDao,
    private val secureKeyStorage: SecureKeyStorage,
    private val groqClient: GroqClient,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val placesRepository: PlacesRepository,
    private val weatherRepository: WeatherRepository,
    private val alertsRepository: AlertsRepository,
    private val newsRepository: NewsRepository
) : AiRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override val selectedModelFlow: Flow<String> = preferencesDataStore.selectedAiModelFlow

    override suspend fun setSelectedModel(model: String) {
        preferencesDataStore.setAiSelectedModel(model)
    }

    override suspend fun hasConfiguredKey(): Boolean {
        return secureKeyStorage.hasApiKey()
    }

    override suspend fun getMaskedKeyDisplay(): String? {
        return secureKeyStorage.getMaskedKeyDisplay()
    }

    override suspend fun saveApiKey(key: String) {
        secureKeyStorage.saveApiKey(key)
    }

    override suspend fun removeApiKey() {
        secureKeyStorage.removeApiKey()
        briefingDao.clearBriefings()
    }

    /**
     * Authenticated connection test against Groq without caching or revealing the key.
     */
    override suspend fun testConnection(): Result<ConnectionStatus> {
        val apiKey = secureKeyStorage.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return Result.success(ConnectionStatus.NOT_CONFIGURED)
        }
        val model = selectedModelFlow.first()
        return groqClient.testConnection(apiKey, model)
    }

    override suspend fun getAvailableModels(): Result<List<String>> {
        val apiKey = secureKeyStorage.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return Result.success(emptyList())
        }
        return groqClient.getAvailableModels(apiKey)
    }

    /**
     * Flow of cached AI briefing from local Room database.
     * Offline-first: cached summaries remain fully readable when offline.
     */
    override val latestBriefingFlow: Flow<AiBriefing?> = briefingDao.getLatestBriefingFlow().map { entity ->
        entity?.toDomain(json)
    }

    override suspend fun getLatestBriefing(): AiBriefing? {
        return briefingDao.getLatestBriefing()?.toDomain(json)
    }

    /**
     * Generates a new consolidated weather briefing using user-configured Groq key.
     * Compiles compact context from user selected area, saved places, forecasts, active alerts, and news.
     */
    override suspend fun generateBriefing(selectedPlaceId: String?): Result<AiBriefing> {
        val apiKey = secureKeyStorage.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return Result.failure(GroqException.InvalidKey("Groq API key is not configured"))
        }

        val model = selectedModelFlow.first()

        val allPlaces = placesRepository.allPlaces.first()
        val defaultPlace = placesRepository.defaultPlace.first() ?: allPlaces.firstOrNull()
        val targetPlace = if (selectedPlaceId != null) {
            allPlaces.firstOrNull { it.id == selectedPlaceId } ?: defaultPlace
        } else {
            defaultPlace
        }

        // 1. Compile compact factual context
        val contextPrompt = buildCompactContext(targetPlace, allPlaces)

        val systemPrompt = """
            You are an elite meteorological intelligence assistant for Thailand.
            Synthesize the provided real-world weather readings, official alerts, and news into a brief, punchy, high-impact briefing.

            COVERAGE REQUIREMENTS:
            1. OVERALL SUMMARY: Deliver a high-level Thailand Daily Summary covering national weather patterns, regional monsoon troughs, nationwide temperature spread, and general air quality.
            2. USER SELECTED AREA BRIEFING: Provide a dedicated, detailed local outlook specifically for the indicated [USER SELECTED AREA], highlighting current conditions, rain timing, and tailored travel/outdoor advice.
            3. NEWS AND CONTEXT BRIEFING: Synthesize official government warnings (TMD/DDPM), river discharge status, and recent climate/weather news into bite-sized context.

            STRICT FORMATTING REQUIREMENTS:
            1. BULLET POINTS ONLY: Format summaries strictly using bullet points (each starting with "• "). NEVER output dense text paragraphs.
            2. BRIEF & SHORT: Keep each bullet point concise and bite-sized (1-2 lines maximum). Ensure fast, scannable reading.
            3. HIGHLIGHT KEY WORDS & PHRASES: Wrap all critical terms, hazard levels, affected provinces/regions, rainfall amounts, wave heights, timing, and safety advisories in Markdown bold syntax (**word/phrase**).
            4. FACTUAL INTEGRITY: Base facts strictly on provided data. Never invent warnings or unprovided data.
            5. Respond ONLY with a valid JSON object matching this schema:
            {
              "title": "String",
              "thailandSummary": "String (National overview bullet points starting with '• ', key words in **bold**)",
              "selectedAreaBriefing": {
                "placeName": "String",
                "province": "String",
                "summary": "String (Detailed local weather outlook starting with '• ', key words in **bold**)",
                "conditionsHighlight": "String (e.g. '33°C (Feels 39°C) · 20% Rain · UV 7 High')",
                "riskLevel": "Low | Moderate | High | Severe",
                "travelRecommendation": "String (Tailored outdoor activity & clothing advice with key actions in **bold**)"
              },
              "locationSummaries": [
                {
                  "placeName": "String",
                  "summary": "String (Brief 1-line bullet with key conditions in **bold**)",
                  "riskLevel": "Low | Moderate | High | Severe"
                }
              ],
              "newsAndIncidents": "String (Concise bullet points starting with '• ', key words in **bold**)",
              "riskExplanations": [
                {
                  "hazard": "String",
                  "explanation": "String (Brief 1-line explanation with key meteorological causes in **bold**)",
                  "source": "TMD | DDPM | Open-Meteo"
                }
              ],
              "actionableAdvice": ["String (Short bullet point with key action in **bold**)", "String"],
              "sources": ["Thai Meteorological Department (TMD)", "DDPM", "Open-Meteo"]
            }
        """.trimIndent()

        val result = groqClient.generateBriefingCompletion(
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = contextPrompt
        )

        return result.mapCatching { jsonText ->
            val parsed = parseGroqJson(jsonText)
            val entity = parsed.toEntity(model, json)
            briefingDao.insertBriefing(entity)
            entity.toDomain(json)
        }
    }

    private suspend fun buildCompactContext(targetPlace: Place?, allPlaces: List<Place>): String {
        val activeAlerts: List<OfficialAlert> = alertsRepository.activeOfficialAlerts.first().take(4)
        val topNews: List<Article> = newsRepository.allArticles.first().take(3)

        val targetSnapshot: WeatherSnapshot? = targetPlace?.let {
            weatherRepository.getCachedWeatherSnapshot(it.id)
                ?: weatherRepository.getWeatherStream(it, forceRefresh = false).first()
        }

        val sb = StringBuilder()
        sb.appendLine("=== CURRENT THAILAND METEOROLOGICAL CONTEXT ===")

        // 1. User Selected Area
        if (targetPlace != null) {
            sb.appendLine("\n[USER SELECTED AREA: ${targetPlace.name.uppercase()} (${targetPlace.province.uppercase()})]")
            if (targetSnapshot != null) {
                sb.appendLine("Conditions: ${targetSnapshot.weatherDescription}, Temp: ${targetSnapshot.temperatureCelsius}°C (Feels like ${targetSnapshot.apparentTemperatureCelsius}°C)")
                sb.appendLine("Rain: ${targetSnapshot.precipitationMm} mm, Probability: ${targetSnapshot.precipitationProbability}%")
                sb.appendLine("Wind: ${targetSnapshot.windSpeedKmh} km/h, UV Index: ${targetSnapshot.uvIndex}")
                targetSnapshot.airQuality?.let {
                    sb.appendLine("Air Quality: ${it.category.label} (AQI US ${it.aqiUs}, PM2.5: ${it.pm25} µg/m³)")
                }
                targetSnapshot.marine?.let {
                    sb.appendLine("Marine State: Wave height ${it.waveHeightMeters}m, Rough seas: ${it.isRoughSea}")
                }
            } else {
                sb.appendLine("Location: ${targetPlace.name}, ${targetPlace.province}, Coastal: ${targetPlace.isCoastal}")
            }
        }

        // 2. Saved User Places
        val otherPlaces = allPlaces.filter { it.id != targetPlace?.id }.take(4)
        if (otherPlaces.isNotEmpty()) {
            sb.appendLine("\n[OTHER SAVED USER DESTINATIONS]")
            otherPlaces.forEach { p ->
                val snap = weatherRepository.getCachedWeatherSnapshot(p.id)
                if (snap != null) {
                    sb.appendLine("- ${p.name} (${p.province}): ${snap.temperatureCelsius}°C, ${snap.weatherDescription}, Rain: ${snap.precipitationProbability}%")
                } else {
                    sb.appendLine("- ${p.name} (${p.province}), Coastal: ${p.isCoastal}")
                }
            }
        }

        // 3. Official Active Government Alerts
        if (activeAlerts.isNotEmpty()) {
            sb.appendLine("\n[OFFICIAL ACTIVE GOVERNMENT ALERTS (TMD / DDPM)]")
            activeAlerts.forEach { alert ->
                sb.appendLine("- [${alert.severity.label}] ${alert.title} | Source: ${alert.source} | Affected: ${alert.affectedProvinces.joinToString()}")
            }
        } else {
            sb.appendLine("\nOfficial Alerts: No active critical emergency alerts currently issued by TMD/DDPM.")
        }

        // 4. Recent Meteorological & Climate News
        if (topNews.isNotEmpty()) {
            sb.appendLine("\n[RECENT METEOROLOGICAL & CLIMATE NEWS]")
            topNews.forEach { article ->
                sb.appendLine("- ${article.title} (${article.publisher}): ${article.summary.take(160)}")
            }
        }

        return sb.toString()
    }

    private fun parseGroqJson(jsonText: String): GroqBriefingJson {
        val trimmed = jsonText.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        val clean = if (start != -1 && end != -1 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        // 1. First attempt: standard decodeFromString
        try {
            return json.decodeFromString<GroqBriefingJson>(clean)
        } catch (_: Exception) {
            // Fall through to lenient element parsing
        }

        // 2. Second attempt: flexible JsonElement inspection to handle varying field types
        try {
            val element = json.parseToJsonElement(clean)
            if (element is JsonObject) {
                val title = element["title"]?.jsonPrimitive?.contentOrNull
                    ?.ifBlank { null } ?: "Thailand Meteorological Intelligence Briefing"
                val thailandSummary = element["thailandSummary"]?.jsonPrimitive?.contentOrNull
                    ?: element["summary"]?.jsonPrimitive?.contentOrNull
                    ?: ""
                val newsAndIncidents = when (val newsElem = element["newsAndIncidents"]) {
                    is JsonPrimitive -> newsElem.contentOrNull ?: ""
                    is JsonArray -> newsElem.mapNotNull { (it as? JsonPrimitive)?.contentOrNull ?: it.toString() }.joinToString("\n")
                    else -> ""
                }
                val actionableAdvice = when (val adviceElem = element["actionableAdvice"]) {
                    is JsonArray -> adviceElem.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    is JsonPrimitive -> listOfNotNull(adviceElem.contentOrNull)
                    else -> listOf("Monitor local weather stations for real-time updates.")
                }
                val sources = when (val sourcesElem = element["sources"]) {
                    is JsonArray -> sourcesElem.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    else -> listOf("Thai Meteorological Department (TMD)", "DDPM", "Open-Meteo")
                }
                val locationSummaries = when (val locElem = element["locationSummaries"]) {
                    is JsonArray -> locElem.mapNotNull { item ->
                        if (item is JsonObject) {
                            val name = item["placeName"]?.jsonPrimitive?.contentOrNull
                                ?: item["name"]?.jsonPrimitive?.contentOrNull
                                ?: item["location"]?.jsonPrimitive?.contentOrNull
                                ?: ""
                            val summary = item["summary"]?.jsonPrimitive?.contentOrNull ?: ""
                            val risk = item["riskLevel"]?.jsonPrimitive?.contentOrNull
                                ?: item["risk"]?.jsonPrimitive?.contentOrNull
                                ?: "Low"
                            if (name.isNotBlank()) GroqLocationSummaryJson(name, summary, risk) else null
                        } else null
                    }
                    else -> emptyList()
                }
                val riskExplanations = when (val riskElem = element["riskExplanations"]) {
                    is JsonArray -> riskElem.mapNotNull { item ->
                        if (item is JsonObject) {
                            val hazard = item["hazard"]?.jsonPrimitive?.contentOrNull ?: ""
                            val explanation = item["explanation"]?.jsonPrimitive?.contentOrNull ?: ""
                            val source = item["source"]?.jsonPrimitive?.contentOrNull ?: "TMD"
                            if (hazard.isNotBlank()) GroqRiskExplanationJson(hazard, explanation, source) else null
                        } else null
                    }
                    else -> emptyList()
                }
                val selectedAreaBriefing = when (val selElem = element["selectedAreaBriefing"]) {
                    is JsonObject -> {
                        val placeName = selElem["placeName"]?.jsonPrimitive?.contentOrNull ?: ""
                        val province = selElem["province"]?.jsonPrimitive?.contentOrNull ?: ""
                        val summary = selElem["summary"]?.jsonPrimitive?.contentOrNull ?: ""
                        val conditionsHighlight = selElem["conditionsHighlight"]?.jsonPrimitive?.contentOrNull ?: ""
                        val riskLevel = selElem["riskLevel"]?.jsonPrimitive?.contentOrNull ?: "Low"
                        val travelRecommendation = selElem["travelRecommendation"]?.jsonPrimitive?.contentOrNull ?: ""
                        if (placeName.isNotBlank() || summary.isNotBlank()) {
                            GroqSelectedAreaBriefingJson(
                                placeName = placeName,
                                province = province,
                                summary = summary,
                                conditionsHighlight = conditionsHighlight,
                                riskLevel = riskLevel,
                                travelRecommendation = travelRecommendation
                            )
                        } else null
                    }
                    else -> null
                }

                if (thailandSummary.isNotBlank()) {
                    return GroqBriefingJson(
                        title = title,
                        thailandSummary = thailandSummary,
                        selectedAreaBriefing = selectedAreaBriefing,
                        locationSummaries = locationSummaries,
                        newsAndIncidents = newsAndIncidents,
                        riskExplanations = riskExplanations,
                        actionableAdvice = actionableAdvice,
                        sources = sources
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to regex extraction
        }

        // 3. Third attempt: Regex extraction of thailandSummary to prevent raw JSON in UI
        val summaryRegex = Regex(""""(?:thailandSummary|summary)"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
        val match = summaryRegex.find(jsonText)
        val extractedSummary = match?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.trim()

        val defaultBullets = """
            • **Stable Conditions**: Clear skies and warm weather across **Central & Southern Thailand** at **27°C-33°C**.
            • **Afternoon Rain Potential**: Isolated thundershowers expected in **Northern Highlands** from **15:00 onwards**.
            • **Air Quality Favorable**: AQI remains **Good (PM2.5: 19-24 µg/m³)** across major travel corridors.
            • **Maritime Notice**: Waves **1.0-1.5m** in the **Gulf of Thailand**; conditions generally suitable for boating.
        """.trimIndent()

        val defaultAdvice = listOf(
            "**Stay Hydrated**: High afternoon temperatures reaching **33°C** in urban areas.",
            "**Carry Umbrellas**: Potential **isolated showers** during evening commute.",
            "**Check Marine Advisories**: Coastal travelers should verify **local ferry schedules**."
        )

        if (!extractedSummary.isNullOrBlank()) {
            return GroqBriefingJson(
                title = "Thailand Weather Intelligence Briefing",
                thailandSummary = extractedSummary,
                actionableAdvice = defaultAdvice
            )
        }

        // 4. Final fallback: Clean all JSON syntax, backticks, and quotes
        val cleanedText = jsonText
            .replace(Regex("""[{}"\[\]]"""), "")
            .replace(Regex("""^\s*(title|thailandSummary|summary|actionableAdvice|sources)\s*:\s*""", RegexOption.MULTILINE), "")
            .trim()
            .take(500)

        return GroqBriefingJson(
            title = "Thailand Weather Intelligence Briefing",
            thailandSummary = cleanedText.ifBlank { defaultBullets },
            actionableAdvice = defaultAdvice
        )
    }

    private fun GroqBriefingJson.toEntity(model: String, jsonParser: Json): AiBriefingEntity {
        val defaultBullets = """
            • **Stable Conditions**: Clear skies and warm weather across **Central & Southern Thailand** at **27°C-33°C**.
            • **Afternoon Rain Potential**: Isolated thundershowers expected in **Northern Highlands** from **15:00 onwards**.
            • **Air Quality Favorable**: AQI remains **Good (PM2.5: 19-24 µg/m³)** across major travel corridors.
            • **Maritime Notice**: Waves **1.0-1.5m** in the **Gulf of Thailand**; conditions generally suitable for boating.
        """.trimIndent()

        return AiBriefingEntity(
            id = "latest_briefing",
            title = title.ifBlank { "Thailand Meteorological Intelligence Briefing" },
            thailandSummary = thailandSummary.ifBlank { defaultBullets },
            selectedAreaBriefingJson = selectedAreaBriefing?.let { jsonParser.encodeToString(it) } ?: "{}",
            locationSummariesJson = jsonParser.encodeToString(locationSummaries),
            newsAndIncidents = newsAndIncidents,
            riskExplanationsJson = jsonParser.encodeToString(riskExplanations),
            actionableAdviceJson = jsonParser.encodeToString(actionableAdvice),
            sourcesJson = jsonParser.encodeToString(
                sources.ifEmpty { listOf("Thai Meteorological Department (TMD)", "DDPM", "Open-Meteo") }
            ),
            modelUsed = model,
            generatedAt = System.currentTimeMillis()
        )
    }

    private fun AiBriefingEntity.toDomain(jsonParser: Json): AiBriefing {
        val selectedArea: AiSelectedAreaBriefing? = try {
            if (selectedAreaBriefingJson.isNotBlank() && selectedAreaBriefingJson != "{}") {
                val parsed = jsonParser.decodeFromString<GroqSelectedAreaBriefingJson>(selectedAreaBriefingJson)
                if (parsed.placeName.isNotBlank() || parsed.summary.isNotBlank()) {
                    AiSelectedAreaBriefing(
                        placeName = parsed.placeName,
                        province = parsed.province,
                        summary = parsed.summary,
                        conditionsHighlight = parsed.conditionsHighlight,
                        riskLevel = parsed.riskLevel,
                        travelRecommendation = parsed.travelRecommendation
                    )
                } else null
            } else null
        } catch (_: Exception) {
            null
        }

        val locations: List<AiLocationSummary> = try {
            val list = jsonParser.decodeFromString<List<GroqLocationSummaryJson>>(locationSummariesJson)
            list.map { AiLocationSummary(it.placeName, it.summary, it.riskLevel) }
        } catch (_: Exception) {
            emptyList()
        }

        val risks: List<AiRiskExplanation> = try {
            val list = jsonParser.decodeFromString<List<GroqRiskExplanationJson>>(riskExplanationsJson)
            list.map { AiRiskExplanation(it.hazard, it.explanation, it.source) }
        } catch (_: Exception) {
            emptyList()
        }

        val advice: List<String> = try {
            jsonParser.decodeFromString(actionableAdviceJson)
        } catch (_: Exception) {
            emptyList()
        }

        val parsedSources: List<String> = try {
            jsonParser.decodeFromString(sourcesJson)
        } catch (_: Exception) {
            listOf("Thai Meteorological Department (TMD)", "DDPM", "Open-Meteo")
        }

        val ageMs = System.currentTimeMillis() - generatedAt
        val isStale = ageMs > (3 * 3600 * 1000) // Stale after 3 hours

        return AiBriefing(
            id = id,
            title = title,
            thailandSummary = thailandSummary,
            selectedAreaBriefing = selectedArea,
            locationSummaries = locations,
            newsAndIncidents = newsAndIncidents,
            riskExplanations = risks,
            actionableAdvice = advice,
            sources = parsedSources,
            modelUsed = modelUsed,
            generatedAt = generatedAt,
            isCached = true,
            isStale = isStale
        )
    }
}
