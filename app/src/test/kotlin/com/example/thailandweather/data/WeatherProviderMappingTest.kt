package com.example.thailandweather.data

import com.example.thailandweather.core.database.dao.WeatherCacheDao
import com.example.thailandweather.core.database.entity.WeatherCacheEntity
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.model.WeatherSnapshot
import com.example.thailandweather.data.provider.WeatherProvider
import com.example.thailandweather.data.remote.OpenMeteoCurrentDto
import com.example.thailandweather.data.remote.OpenMeteoForecastResponse
import com.example.thailandweather.data.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherProviderMappingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testOpenMeteoForecastSerialization() {
        val rawJson = """
        {
            "latitude": 13.75,
            "longitude": 100.5,
            "current": {
                "time": "2026-10-26T12:00",
                "temperature_2m": 32.5,
                "relative_humidity_2m": 72,
                "apparent_temperature": 38.0,
                "precipitation": 1.2,
                "weather_code": 80,
                "wind_speed_10m": 14.5,
                "wind_direction_10m": 195,
                "surface_pressure": 1010.5,
                "uv_index": 7.2
            }
        }
        """.trimIndent()

        val parsed = json.decodeFromString<OpenMeteoForecastResponse>(rawJson)
        assertNotNull(parsed.current)
        assertEquals(32.5, parsed.current!!.temperature2m, 0.01)
        assertEquals(72, parsed.current!!.relativeHumidity2m)
        assertEquals(38.0, parsed.current!!.apparentTemperature, 0.01)
        assertEquals(80, parsed.current!!.weatherCode)
        assertEquals(14.5, parsed.current!!.windSpeed10m, 0.01)
    }

    @Test
    fun testOfflineFallbackWhenNetworkFailsAndNoCache() = runTest {
        val place = Place("th_bkk", "Bangkok", null, "Bangkok", null, 13.7563, 100.5018)

        val emptyCacheDao = object : WeatherCacheDao {
            override suspend fun getCachedWeather(placeId: String): WeatherCacheEntity? = null
            override fun getCachedWeatherFlow(placeId: String): Flow<WeatherCacheEntity?> = flowOf(null)
            override suspend fun getAllCached(): List<WeatherCacheEntity> = emptyList()
            override suspend fun insertOrUpdate(cache: WeatherCacheEntity) {}
            override suspend fun deleteExpired(now: Long) {}
            override suspend fun deleteByPlaceId(placeId: String) {}
        }

        val failingProvider = object : WeatherProvider {
            override val providerId: String = "test_failing"
            override val providerName: String = "Test Failing Provider"
            override val attributionNotice: String = "Test"
            override val termsUrl: String = "https://example.com"

            override suspend fun fetchWeatherSnapshot(place: Place, includeAirQuality: Boolean, includeMarine: Boolean): Result<WeatherSnapshot> {
                return Result.failure(java.io.IOException("Network unavailable"))
            }

            override suspend fun fetchHourlyForecast(place: Place) = Result.failure<List<com.example.thailandweather.core.model.HourlyForecast>>(java.io.IOException("Network error"))
            override suspend fun fetchDailyForecast(place: Place) = Result.failure<List<com.example.thailandweather.core.model.DailyForecast>>(java.io.IOException("Network error"))
            override suspend fun searchLocations(query: String) = Result.success(emptyList<Place>())
        }

        val repository = WeatherRepository(emptyCacheDao, failingProvider)
        val snapshot = repository.getWeatherStream(place, forceRefresh = false).first()

        assertNotNull(snapshot)
        assertTrue("Fallback snapshot must be marked as stale/offline", snapshot.isStale)
        assertEquals("Bangkok", snapshot.placeName)
    }

    @Test
    fun testStaleFlagAppliedWhenCacheExpired() = runTest {
        val place = Place("th_bkk", "Bangkok", null, "Bangkok", null, 13.7563, 100.5018)
        val oldSnapshot = WeatherSnapshot(
            placeId = place.id,
            placeName = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
            timestamp = System.currentTimeMillis() - 7200_000L,
            temperatureCelsius = 29.0,
            apparentTemperatureCelsius = 33.0,
            relativeHumidity = 70,
            precipitationMm = 0.0,
            precipitationProbability = 0,
            weatherCode = 1,
            weatherDescription = "Clear",
            windSpeedKmh = 10.0,
            windDirectionDegrees = 180,
            uvIndex = 5.0,
            isStale = false
        )

        val expiredCacheDao = object : WeatherCacheDao {
            override suspend fun getCachedWeather(placeId: String): WeatherCacheEntity {
                return WeatherCacheEntity(
                    placeId = place.id,
                    dataJson = json.encodeToString(oldSnapshot),
                    cachedAt = System.currentTimeMillis() - 7200_000L,
                    expiresAt = System.currentTimeMillis() - 3600_000L, // Expired 1h ago
                    isStale = false
                )
            }
            override fun getCachedWeatherFlow(placeId: String): Flow<WeatherCacheEntity?> = flowOf(null)
            override suspend fun getAllCached(): List<WeatherCacheEntity> = emptyList()
            override suspend fun insertOrUpdate(cache: WeatherCacheEntity) {}
            override suspend fun deleteExpired(now: Long) {}
            override suspend fun deleteByPlaceId(placeId: String) {}
        }

        val mockProvider = object : WeatherProvider {
            override val providerId: String = "mock"
            override val providerName: String = "Mock Provider"
            override val attributionNotice: String = "Mock"
            override val termsUrl: String = ""
            override suspend fun fetchWeatherSnapshot(place: Place, includeAirQuality: Boolean, includeMarine: Boolean) = Result.failure<WeatherSnapshot>(java.io.IOException("Server 500"))
            override suspend fun fetchHourlyForecast(place: Place) = Result.success(emptyList<com.example.thailandweather.core.model.HourlyForecast>())
            override suspend fun fetchDailyForecast(place: Place) = Result.success(emptyList<com.example.thailandweather.core.model.DailyForecast>())
            override suspend fun searchLocations(query: String) = Result.success(emptyList<Place>())
        }

        val repository = WeatherRepository(expiredCacheDao, mockProvider)
        val streamSnapshot = repository.getWeatherStream(place, forceRefresh = false).first()

        assertTrue("Expired cache should be emitted with isStale = true", streamSnapshot.isStale)
    }
}
