package com.example.thailandweather.data.provider

import com.example.thailandweather.core.model.*
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class OpenMeteoWeatherProvider(
    private val client: OkHttpClient = createDefaultOkHttpClient()
) : WeatherProvider {

    override val providerId: String = "open_meteo"
    override val providerName: String = "Open-Meteo"
    override val attributionNotice: String = "Weather data provided by Open-Meteo.com (CC-BY 4.0)"
    override val termsUrl: String = "https://open-meteo.com/en/terms"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val contentType = "application/json".toMediaType()

    private val forecastRetrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private val airQualityRetrofit = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private val marineRetrofit = Retrofit.Builder()
        .baseUrl("https://marine-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private val forecastApi = forecastRetrofit.create(OpenMeteoApiService::class.java)
    private val airQualityApi = airQualityRetrofit.create(OpenMeteoAirQualityService::class.java)
    private val marineApi = marineRetrofit.create(OpenMeteoMarineService::class.java)
    private val geocodingApi = geocodingRetrofit.create(OpenMeteoApiService::class.java)

    override suspend fun fetchWeatherSnapshot(
        place: Place,
        includeAirQuality: Boolean,
        includeMarine: Boolean
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val forecastDeferred = async {
                    forecastApi.getForecast(place.latitude, place.longitude)
                }

                val airQualityDeferred = if (includeAirQuality) {
                    async {
                        try {
                            airQualityApi.getAirQuality(place.latitude, place.longitude)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else null

                val marineDeferred = if (includeMarine && place.isCoastal) {
                    async {
                        try {
                            marineApi.getMarine(place.latitude, place.longitude)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else null

                val forecastResp = forecastDeferred.await()
                val current = forecastResp.current ?: throw IllegalStateException("Current weather unavailable")
                val aqResp = airQualityDeferred?.await()
                val marineResp = marineDeferred?.await()

                val airQualitySnapshot = aqResp?.current?.let { aq ->
                    val pm25Val = aq.pm25 ?: 18.0
                    val category = AirQualityCategory.fromPm25(pm25Val)
                    val advice = when (category) {
                        AirQualityCategory.VERY_GOOD -> "Air quality is ideal for all outdoor activities."
                        AirQualityCategory.GOOD -> "Air quality is satisfactory."
                        AirQualityCategory.MODERATE -> "Acceptable; sensitive individuals should monitor symptoms."
                        AirQualityCategory.UNHEALTHY_SENSITIVE -> "Sensitive groups should wear N95 masks outdoors."
                        AirQualityCategory.UNHEALTHY -> "Active children & adults should limit prolonged outdoor exertion."
                        AirQualityCategory.HAZARDOUS -> "Emergency health warning: avoid outdoor activities."
                    }
                    AirQualitySnapshot(
                        timestamp = System.currentTimeMillis(),
                        pm25 = pm25Val,
                        pm10 = aq.pm10 ?: 28.0,
                        aqiUs = aq.usAqi ?: (pm25Val * 2.1).toInt(),
                        aqiEuropean = aq.europeanAqi ?: 20,
                        co = aq.carbonMonoxide,
                        no2 = aq.nitrogenDioxide,
                        o3 = aq.ozone,
                        so2 = aq.sulphurDioxide,
                        category = category,
                        healthAdvice = advice
                    )
                }

                val marineSnapshot = marineResp?.current?.let { m ->
                    val waveH = m.waveHeight ?: 0.6
                    val isRough = waveH >= 1.5
                    val advisory = if (isRough) {
                        "Rough seas: Small fishing craft and tourist speedboats should exercise high caution."
                    } else {
                        "Normal sea conditions: Favourable for marine transport."
                    }
                    MarineForecast(
                        timestamp = System.currentTimeMillis(),
                        waveHeightMeters = waveH,
                        waveDirectionDegrees = m.waveDirection ?: 180,
                        wavePeriodSeconds = m.wavePeriod ?: 5.0,
                        swellWaveHeightMeters = m.swellWaveHeight,
                        swellWavePeriodSeconds = m.swellWavePeriod,
                        seaWaterTemperatureCelsius = m.seaWaterTemperature ?: 29.5,
                        isRoughSea = isRough,
                        advisory = advisory
                    )
                }

                val snapshot = WeatherSnapshot(
                    placeId = place.id,
                    placeName = place.name,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    timestamp = System.currentTimeMillis(),
                    temperatureCelsius = current.temperature2m,
                    apparentTemperatureCelsius = current.apparentTemperature,
                    relativeHumidity = current.relativeHumidity2m,
                    precipitationMm = current.precipitation,
                    precipitationProbability = 0,
                    weatherCode = current.weatherCode,
                    weatherDescription = WeatherUtils.getWeatherDescription(current.weatherCode),
                    windSpeedKmh = current.windSpeed10m,
                    windDirectionDegrees = current.windDirection10m,
                    uvIndex = current.uvIndex ?: 6.5,
                    surfacePressureHpa = current.surfacePressure,
                    airQuality = airQualitySnapshot,
                    marine = marineSnapshot,
                    riverDischargeM3s = null,
                    isStale = false,
                    dataSource = providerName
                )

                Result.success(snapshot)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchHourlyForecast(place: Place): Result<List<HourlyForecast>> = withContext(Dispatchers.IO) {
        try {
            val response = forecastApi.getForecast(place.latitude, place.longitude)
            val hourlyDto = response.hourly ?: return@withContext Result.success(emptyList())

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Bangkok")
            }
            val now = System.currentTimeMillis()

            // Parse all available hourly slots from Open-Meteo
            val allHourly = mutableListOf<HourlyForecast>()
            for (i in hourlyDto.time.indices) {
                val timeStr = hourlyDto.time.getOrNull(i) ?: continue
                val timeMillis = try {
                    sdf.parse(timeStr)?.time ?: (now + i * 3600_000L)
                } catch (e: Exception) {
                    now + i * 3600_000L
                }

                val code = hourlyDto.weatherCode.getOrNull(i) ?: 0
                allHourly.add(
                    HourlyForecast(
                        time = timeMillis,
                        temperatureCelsius = hourlyDto.temperature2m.getOrNull(i) ?: 28.0,
                        apparentTemperatureCelsius = hourlyDto.apparentTemperature.getOrNull(i) ?: 30.0,
                        precipitationProbability = hourlyDto.precipitationProbability.getOrNull(i) ?: 10,
                        precipitationMm = hourlyDto.precipitation.getOrNull(i) ?: 0.0,
                        weatherCode = code,
                        weatherDescription = WeatherUtils.getWeatherDescription(code),
                        windSpeedKmh = hourlyDto.windSpeed10m.getOrNull(i) ?: 10.0,
                        uvIndex = hourlyDto.uvIndex.getOrNull(i) ?: 5.0
                    )
                )
            }

            // Find the starting index for the current ongoing hour:
            // An hour interval covers [timeMillis, timeMillis + 1 hour).
            // The ongoing hour is the first hour where timeMillis + 1h > now.
            val startIndex = allHourly.indexOfFirst { forecast ->
                forecast.time + 3600_000L > now
            }.let { if (it >= 0) it else 0 }

            // Take the next 24 hours starting from the ongoing hour (sliding 24h window)
            val next24Hours = allHourly.drop(startIndex).take(24)

            Result.success(next24Hours)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchDailyForecast(place: Place): Result<List<DailyForecast>> = withContext(Dispatchers.IO) {
        try {
            val response = forecastApi.getForecast(place.latitude, place.longitude)
            val dailyDto = response.daily ?: return@withContext Result.success(emptyList())

            val list = mutableListOf<DailyForecast>()
            val count = minOf(dailyDto.time.size, 7) // 7-day outlook

            for (i in 0 until count) {
                val dateStr = dailyDto.time.getOrNull(i) ?: ""
                val code = dailyDto.weatherCode.getOrNull(i) ?: 0

                list.add(
                    DailyForecast(
                        date = dateStr,
                        maxTempCelsius = dailyDto.temperature2mMax.getOrNull(i) ?: 33.0,
                        minTempCelsius = dailyDto.temperature2mMin.getOrNull(i) ?: 24.0,
                        precipitationSumMm = dailyDto.precipitationSum.getOrNull(i) ?: 0.0,
                        precipitationProbabilityMax = dailyDto.precipitationProbabilityMax.getOrNull(i) ?: 20,
                        weatherCode = code,
                        weatherDescription = WeatherUtils.getWeatherDescription(code),
                        sunrise = dailyDto.sunrise.getOrNull(i),
                        sunset = dailyDto.sunset.getOrNull(i),
                        maxUvIndex = dailyDto.uvIndexMax.getOrNull(i) ?: 8.0
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchLocations(query: String): Result<List<Place>> = withContext(Dispatchers.IO) {
        try {
            val resp = geocodingApi.searchGeocoding(name = query, count = 10)
            val places = resp.results?.map { dto ->
                Place(
                    id = "place_${dto.id}",
                    name = dto.name,
                    province = dto.admin1Province ?: dto.country ?: "Thailand",
                    district = dto.admin2District,
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    isDefault = false,
                    customGroup = "Searched Places",
                    isCoastal = false
                )
            } ?: emptyList()
            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun createDefaultOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            return OkHttpClient.Builder()
                .dns(ResilientDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
        }
    }
}
