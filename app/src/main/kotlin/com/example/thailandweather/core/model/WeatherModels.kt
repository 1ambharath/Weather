package com.example.thailandweather.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a saved or queried location in Thailand.
 */
@Serializable
data class Place(
    val id: String,
    val name: String,
    val thaiName: String? = null,
    val province: String,
    val district: String? = null,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false,
    val customGroup: String? = null,
    val isCoastal: Boolean = false,
    val alertEnabled: Boolean = true,
    val proximityRadiusKm: Int = 25,
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Normalized weather snapshot combining current observations and key indices.
 */
@Serializable
data class WeatherSnapshot(
    val placeId: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val temperatureCelsius: Double,
    val apparentTemperatureCelsius: Double,
    val relativeHumidity: Int,
    val precipitationMm: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val weatherDescription: String,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val uvIndex: Double,
    val surfacePressureHpa: Double? = null,
    val airQuality: AirQualitySnapshot? = null,
    val marine: MarineForecast? = null,
    val riverDischargeM3s: Double? = null,
    val isStale: Boolean = false,
    val dataSource: String = "Open-Meteo"
)

@Serializable
data class HourlyForecast(
    val time: Long,
    val temperatureCelsius: Double,
    val apparentTemperatureCelsius: Double,
    val precipitationProbability: Int,
    val precipitationMm: Double,
    val weatherCode: Int,
    val weatherDescription: String,
    val windSpeedKmh: Double,
    val uvIndex: Double
)

@Serializable
data class DailyForecast(
    val date: String,
    val maxTempCelsius: Double,
    val minTempCelsius: Double,
    val precipitationSumMm: Double,
    val precipitationProbabilityMax: Int,
    val weatherCode: Int,
    val weatherDescription: String,
    val sunrise: String? = null,
    val sunset: String? = null,
    val maxUvIndex: Double
)

@Serializable
data class AirQualitySnapshot(
    val timestamp: Long,
    val pm25: Double,
    val pm10: Double,
    val aqiUs: Int,
    val aqiEuropean: Int,
    val co: Double? = null,
    val no2: Double? = null,
    val o3: Double? = null,
    val so2: Double? = null,
    val category: AirQualityCategory,
    val healthAdvice: String
)

enum class AirQualityCategory(val label: String, val thaiLabel: String, val hexColor: Long) {
    VERY_GOOD("Very Good", "ดีมาก", 0xFF00B0FF),
    GOOD("Good", "ดี", 0xFF00E676),
    MODERATE("Moderate", "ปานกลาง", 0xFFFFD600),
    UNHEALTHY_SENSITIVE("Unhealthy for Sensitive", "เริ่มมีผลต่อสุขภาพ", 0xFFFF9100),
    UNHEALTHY("Unhealthy", "มีผลกระทบต่อสุขภาพ", 0xFFFF1744),
    HAZARDOUS("Hazardous", "อันตราย", 0xFFD50000);

    companion object {
        fun fromPm25(pm25: Double): AirQualityCategory = when {
            pm25 <= 25.0 -> VERY_GOOD
            pm25 <= 37.0 -> GOOD
            pm25 <= 50.0 -> MODERATE
            pm25 <= 90.0 -> UNHEALTHY_SENSITIVE
            pm25 <= 150.0 -> UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

@Serializable
data class MarineForecast(
    val timestamp: Long,
    val waveHeightMeters: Double,
    val waveDirectionDegrees: Int,
    val wavePeriodSeconds: Double,
    val swellWaveHeightMeters: Double? = null,
    val swellWavePeriodSeconds: Double? = null,
    val seaWaterTemperatureCelsius: Double? = null,
    val isRoughSea: Boolean = false,
    val advisory: String? = null
)

/**
 * Official warning from TMD / DDPM authorities.
 */
@Serializable
data class OfficialAlert(
    val id: String,
    val title: String,
    val titleThai: String? = null,
    val description: String,
    val descriptionThai: String? = null,
    val severity: AlertSeverity,
    val source: String = "TMD (Thai Meteorological Department)",
    val sourceUrl: String? = null,
    val issuedAt: Long,
    val expiresAt: Long,
    val affectedProvinces: List<String>,
    val instruction: String? = null,
    val isActive: Boolean = true
)

/**
 * App-generated forecast risk based on meteorological thresholds.
 */
@Serializable
data class ForecastRisk(
    val id: String,
    val placeId: String,
    val placeName: String,
    val riskType: RiskType,
    val severity: AlertSeverity,
    val headline: String,
    val detail: String,
    val triggerValue: String,
    val estimatedWindowStart: Long,
    val estimatedWindowEnd: Long
)

enum class RiskType(val label: String) {
    EXTREME_HEAT("Extreme Heat Index"),
    TORRENTIAL_RAIN("Torrential Rain & Flooding Risk"),
    STRONG_WIND_GALE("Gale Force Winds"),
    ROUGH_SEAS("Hazardous Sea Conditions"),
    HIGH_PM25("Hazardous Air Pollution"),
    SEVERE_THUNDERSTORM("Severe Thunderstorm & Hail")
}

enum class AlertSeverity(val level: Int, val label: String, val hexColor: Long) {
    ADVISORY(1, "Advisory", 0xFF29B6F6),
    WATCH(2, "Watch", 0xFFFFCA28),
    WARNING(3, "Warning", 0xFFFF7043),
    EMERGENCY(4, "Emergency", 0xFFD32F2F)
}

/**
 * Thailand weather and climate news article.
 */
@Serializable
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val publisher: String,
    val publishedAt: Long,
    val url: String,
    val category: String,
    val province: String? = null,
    val imageUrl: String? = null,
    val isBookmarked: Boolean = false
)

/**
 * User Preferences.
 */
@Serializable
data class UserPreferences(
    val tempUnitCelsius: Boolean = true,
    val windUnitKmh: Boolean = true,
    val languageThai: Boolean = false,
    val proximityRadiusKm: Int = 25,
    val notificationsEnabled: Boolean = true,
    val notifyOfficialWarnings: Boolean = true,
    val notifyForecastRisks: Boolean = true,
    val notifyAirQuality: Boolean = true,
    val autoRefreshIntervalMinutes: Int = 30
)
