package com.example.thailandweather.data

import com.example.thailandweather.core.database.dao.AlertsDao
import com.example.thailandweather.core.database.entity.AlertEntity
import com.example.thailandweather.core.model.*
import com.example.thailandweather.data.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsEvaluationTest {

    private val fakeDao = object : AlertsDao {
        override fun getAllActiveAlertsFlow(): Flow<List<AlertEntity>> = flowOf(emptyList())
        override fun getOfficialAlertsFlow(): Flow<List<AlertEntity>> = flowOf(emptyList())
        override fun getForecastRisksFlow(): Flow<List<AlertEntity>> = flowOf(emptyList())
        override suspend fun insertAlerts(alerts: List<AlertEntity>) {}
        override suspend fun deleteExpiredAlerts(now: Long) {}
        override suspend fun clearAll() {}
    }

    private val repository = AlertsRepository(fakeDao)

    private val samplePlace = Place(
        id = "th_phuket",
        name = "Phuket",
        province = "Phuket",
        latitude = 7.8804,
        longitude = 98.3923,
        isCoastal = true
    )

    @Test
    fun testExtremeHeatRiskTriggeredWhenApparentTempAbove40() = runTest {
        val snapshot = WeatherSnapshot(
            placeId = samplePlace.id,
            placeName = samplePlace.name,
            latitude = samplePlace.latitude,
            longitude = samplePlace.longitude,
            timestamp = System.currentTimeMillis(),
            temperatureCelsius = 36.0,
            apparentTemperatureCelsius = 43.5, // Dangerous heat index
            relativeHumidity = 80,
            precipitationMm = 0.0,
            precipitationProbability = 10,
            weatherCode = 1,
            weatherDescription = "Mainly clear",
            windSpeedKmh = 10.0,
            windDirectionDegrees = 180,
            uvIndex = 9.0
        )

        val risks = repository.evaluateForecastRisks(samplePlace, snapshot)
        assertTrue("Extreme heat risk should be generated", risks.any { it.riskType == RiskType.EXTREME_HEAT })
        val heatRisk = risks.first { it.riskType == RiskType.EXTREME_HEAT }
        assertEquals(AlertSeverity.WARNING, heatRisk.severity)
    }

    @Test
    fun testTorrentialRainRiskTriggeredWhenPrecipitationHigh() = runTest {
        val snapshot = WeatherSnapshot(
            placeId = samplePlace.id,
            placeName = samplePlace.name,
            latitude = samplePlace.latitude,
            longitude = samplePlace.longitude,
            timestamp = System.currentTimeMillis(),
            temperatureCelsius = 28.0,
            apparentTemperatureCelsius = 30.0,
            relativeHumidity = 95,
            precipitationMm = 55.0, // Torrential rain
            precipitationProbability = 90,
            weatherCode = 65, // Heavy rain
            weatherDescription = "Heavy rain",
            windSpeedKmh = 25.0,
            windDirectionDegrees = 200,
            uvIndex = 2.0
        )

        val risks = repository.evaluateForecastRisks(samplePlace, snapshot)
        assertTrue("Torrential rain risk should be generated", risks.any { it.riskType == RiskType.TORRENTIAL_RAIN })
        val rainRisk = risks.first { it.riskType == RiskType.TORRENTIAL_RAIN }
        assertEquals(AlertSeverity.EMERGENCY, rainRisk.severity)
    }

    @Test
    fun testRoughSeaRiskTriggeredForCoastalDestinations() = runTest {
        val marine = MarineForecast(
            timestamp = System.currentTimeMillis(),
            waveHeightMeters = 2.8,
            waveDirectionDegrees = 220,
            wavePeriodSeconds = 7.0,
            isRoughSea = true,
            advisory = "High swells"
        )

        val snapshot = WeatherSnapshot(
            placeId = samplePlace.id,
            placeName = samplePlace.name,
            latitude = samplePlace.latitude,
            longitude = samplePlace.longitude,
            timestamp = System.currentTimeMillis(),
            temperatureCelsius = 30.0,
            apparentTemperatureCelsius = 34.0,
            relativeHumidity = 75,
            precipitationMm = 0.0,
            precipitationProbability = 20,
            weatherCode = 2,
            weatherDescription = "Partly cloudy",
            windSpeedKmh = 20.0,
            windDirectionDegrees = 220,
            uvIndex = 6.0,
            marine = marine
        )

        val risks = repository.evaluateForecastRisks(samplePlace, snapshot)
        assertTrue("Rough sea risk should be triggered", risks.any { it.riskType == RiskType.ROUGH_SEAS })
    }
}
