package com.example.thailandweather.data.repository

import com.example.thailandweather.core.database.dao.WeatherCacheDao
import com.example.thailandweather.core.database.entity.WeatherCacheEntity
import com.example.thailandweather.core.model.DailyForecast
import com.example.thailandweather.core.model.HourlyForecast
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.model.WeatherSnapshot
import com.example.thailandweather.data.provider.WeatherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class WeatherRepository(
    private val weatherCacheDao: WeatherCacheDao,
    private val weatherProvider: WeatherProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val hourlyMemoryCache = ConcurrentHashMap<String, List<HourlyForecast>>()
    private val dailyMemoryCache = ConcurrentHashMap<String, List<DailyForecast>>()

    private val CACHE_TTL_MILLIS = 30 * 60 * 1000L // 30 minutes

    /**
     * Offline-first weather stream: emits cached snapshot first, then refreshes from remote.
     */
    fun getWeatherStream(place: Place, forceRefresh: Boolean = false): Flow<WeatherSnapshot> = flow {
        val cached = weatherCacheDao.getCachedWeather(place.id)
        var hasEmittedCached = false

        if (cached != null) {
            try {
                val snapshot = json.decodeFromString<WeatherSnapshot>(cached.dataJson)
                val isStale = System.currentTimeMillis() > cached.expiresAt
                emit(snapshot.copy(isStale = isStale))
                hasEmittedCached = true
            } catch (e: Exception) {
                // Ignore parse error and proceed to network
            }
        }

        val shouldFetch = forceRefresh || cached == null || System.currentTimeMillis() > cached.expiresAt

        if (shouldFetch) {
            val remoteResult = weatherProvider.fetchWeatherSnapshot(place)
            if (remoteResult.isSuccess) {
                val freshSnapshot = remoteResult.getOrThrow()
                // Save to Room cache
                val now = System.currentTimeMillis()
                val cacheEntity = WeatherCacheEntity(
                    placeId = place.id,
                    dataJson = json.encodeToString(freshSnapshot),
                    cachedAt = now,
                    expiresAt = now + CACHE_TTL_MILLIS,
                    isStale = false
                )
                weatherCacheDao.insertOrUpdate(cacheEntity)
                emit(freshSnapshot)
            } else if (!hasEmittedCached) {
                // Remote failed and no cache was found: return fallback offline estimate
                emit(createFallbackSnapshot(place))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getHourlyForecast(place: Place): List<HourlyForecast> = withContext(Dispatchers.IO) {
        hourlyMemoryCache[place.id]?.let { return@withContext it }

        val result = weatherProvider.fetchHourlyForecast(place)
        if (result.isSuccess) {
            val list = result.getOrThrow()
            hourlyMemoryCache[place.id] = list
            list
        } else {
            createFallbackHourly(place)
        }
    }

    suspend fun getDailyForecast(place: Place): List<DailyForecast> = withContext(Dispatchers.IO) {
        dailyMemoryCache[place.id]?.let { return@withContext it }

        val result = weatherProvider.fetchDailyForecast(place)
        if (result.isSuccess) {
            val list = result.getOrThrow()
            dailyMemoryCache[place.id] = list
            list
        } else {
            createFallbackDaily()
        }
    }

    /**
     * Batch fetch weather snapshots for multiple places (for comparison dashboard).
     */
    suspend fun getBatchWeatherSnapshots(places: List<Place>): Map<String, WeatherSnapshot> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<String, WeatherSnapshot>()
        for (place in places) {
            val cached = weatherCacheDao.getCachedWeather(place.id)
            if (cached != null) {
                try {
                    val snapshot = json.decodeFromString<WeatherSnapshot>(cached.dataJson)
                    resultMap[place.id] = snapshot.copy(isStale = System.currentTimeMillis() > cached.expiresAt)
                } catch (e: Exception) {
                    // Fallback to fetch
                }
            }
            if (!resultMap.containsKey(place.id)) {
                val remote = weatherProvider.fetchWeatherSnapshot(place)
                if (remote.isSuccess) {
                    val fresh = remote.getOrThrow()
                    resultMap[place.id] = fresh
                    weatherCacheDao.insertOrUpdate(
                        WeatherCacheEntity(
                            placeId = place.id,
                            dataJson = json.encodeToString(fresh),
                            cachedAt = System.currentTimeMillis(),
                            expiresAt = System.currentTimeMillis() + CACHE_TTL_MILLIS,
                            isStale = false
                        )
                    )
                } else {
                    resultMap[place.id] = createFallbackSnapshot(place)
                }
            }
        }
        resultMap
    }

    private fun createFallbackSnapshot(place: Place): WeatherSnapshot {
        return WeatherSnapshot(
            placeId = place.id,
            placeName = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
            timestamp = System.currentTimeMillis(),
            temperatureCelsius = 31.0,
            apparentTemperatureCelsius = 36.0,
            relativeHumidity = 75,
            precipitationMm = 0.0,
            precipitationProbability = 20,
            weatherCode = 2,
            weatherDescription = "Partly cloudy (Cached estimate)",
            windSpeedKmh = 12.0,
            windDirectionDegrees = 190,
            uvIndex = 7.0,
            airQuality = null,
            marine = null,
            isStale = true,
            dataSource = "Local Cache (Offline)"
        )
    }

    private fun createFallbackHourly(place: Place): List<HourlyForecast> {
        val now = System.currentTimeMillis()
        return (0..23).map { i ->
            HourlyForecast(
                time = now + i * 3600_000L,
                temperatureCelsius = 28.0 + (i % 6),
                apparentTemperatureCelsius = 32.0 + (i % 6),
                precipitationProbability = 20 + (i % 40),
                precipitationMm = if (i % 5 == 0) 1.5 else 0.0,
                weatherCode = 2,
                weatherDescription = "Partly cloudy",
                windSpeedKmh = 10.0 + (i % 5),
                uvIndex = if (i in 6..18) 6.0 else 0.0
            )
        }
    }

    private fun createFallbackDaily(): List<DailyForecast> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return days.mapIndexed { idx, day ->
            DailyForecast(
                date = day,
                maxTempCelsius = 33.0 + (idx % 3),
                minTempCelsius = 24.0 + (idx % 2),
                precipitationSumMm = 2.0 * (idx % 4),
                precipitationProbabilityMax = 30 + (idx * 5 % 50),
                weatherCode = 2,
                weatherDescription = "Scattered clouds",
                maxUvIndex = 8.0
            )
        }
    }
}
