package com.example.thailandweather.core.model

import kotlinx.serialization.Serializable

/**
 * Connection states for user-configured AI providers as specified in NomadKit requirements.
 */
enum class ConnectionStatus(val displayLabel: String) {
    NOT_CONFIGURED("Not configured"),
    KEY_SAVED("Key saved"),
    CHECKING_CONNECTION("Checking connection"),
    CONNECTED("Connected"),
    INVALID_KEY("Invalid key"),
    RATE_LIMIT_EXCEEDED("Rate limit exceeded"),
    CONNECTION_FAILED("Connection failed")
}

/**
 * Supported Groq model configurations with user-friendly descriptions.
 */
@Serializable
data class GroqModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val isRecommended: Boolean = false
) {
    companion object {
        val DEFAULT_MODEL = GroqModelInfo(
            id = "llama-3.3-70b-versatile",
            displayName = "Llama 3.3 70B Versatile",
            description = "High reasoning capability for rich weather synthesis and risk analysis (Recommended)",
            isRecommended = true
        )

        val SUPPORTED_MODELS = listOf(
            DEFAULT_MODEL,
            GroqModelInfo(
                id = "llama-3.1-8b-instant",
                displayName = "Llama 3.1 8B Instant",
                description = "Ultra-low latency inference, best for fast connections on mobile",
                isRecommended = false
            ),
            GroqModelInfo(
                id = "openai/gpt-oss-120b",
                displayName = "OpenAI GPT OSS 120B",
                description = "Advanced open-weight reasoning model hosted on Groq LPU",
                isRecommended = false
            ),
            GroqModelInfo(
                id = "openai/gpt-oss-20b",
                displayName = "OpenAI GPT OSS 20B",
                description = "Fast, efficient reasoning model for quick synthesis",
                isRecommended = false
            )
        )
    }
}

/**
 * Consolidated AI Intelligence Briefing data model.
 */
@Serializable
data class AiBriefing(
    val id: String = "latest_briefing",
    val title: String,
    val thailandSummary: String,
    val selectedAreaBriefing: AiSelectedAreaBriefing? = null,
    val locationSummaries: List<AiLocationSummary> = emptyList(),
    val newsAndIncidents: String = "",
    val riskExplanations: List<AiRiskExplanation> = emptyList(),
    val actionableAdvice: List<String> = emptyList(),
    val sources: List<String> = listOf("Thai Meteorological Department (TMD)", "DDPM", "Open-Meteo"),
    val modelUsed: String = GroqModelInfo.DEFAULT_MODEL.id,
    val generatedAt: Long = System.currentTimeMillis(),
    val isCached: Boolean = false,
    val isStale: Boolean = false
)

/**
 * Detailed briefing for the user's currently selected / active location.
 */
@Serializable
data class AiSelectedAreaBriefing(
    val placeName: String = "",
    val province: String = "",
    val summary: String = "",
    val conditionsHighlight: String = "",
    val riskLevel: String = "Low", // "Low", "Moderate", "High", "Severe"
    val travelRecommendation: String = ""
)

@Serializable
data class AiLocationSummary(
    val placeName: String,
    val summary: String,
    val riskLevel: String = "Low" // "Low", "Moderate", "High", "Severe"
)

@Serializable
data class AiRiskExplanation(
    val hazard: String,
    val explanation: String,
    val source: String
)
