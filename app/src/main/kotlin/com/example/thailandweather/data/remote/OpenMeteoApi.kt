package com.example.thailandweather.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class OpenMeteoForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timezone: String? = null,
    val current: OpenMeteoCurrentDto? = null,
    val hourly: OpenMeteoHourlyDto? = null,
    val daily: OpenMeteoDailyDto? = null
)

@Serializable
data class OpenMeteoCurrentDto(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 0.0,
    val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("wind_speed_10m") val windSpeed10m: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection10m: Int = 0,
    @SerialName("surface_pressure") val surfacePressure: Double? = null,
    @SerialName("uv_index") val uvIndex: Double? = null
)

@Serializable
data class OpenMeteoHourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int> = emptyList(),
    val precipitation: List<Double> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double> = emptyList()
)

@Serializable
data class OpenMeteoDailyDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)

@Serializable
data class OpenMeteoAirQualityResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoAirQualityCurrentDto? = null
)

@Serializable
data class OpenMeteoAirQualityCurrentDto(
    val time: String? = null,
    val pm10: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("carbon_monoxide") val carbonMonoxide: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @SerialName("sulphur_dioxide") val sulphurDioxide: Double? = null,
    val ozone: Double? = null,
    @SerialName("european_aqi") val europeanAqi: Int? = null,
    @SerialName("us_aqi") val usAqi: Int? = null
)

@Serializable
data class OpenMeteoMarineResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoMarineCurrentDto? = null
)

@Serializable
data class OpenMeteoMarineCurrentDto(
    val time: String? = null,
    @SerialName("wave_height") val waveHeight: Double? = null,
    @SerialName("wave_direction") val waveDirection: Int? = null,
    @SerialName("wave_period") val wavePeriod: Double? = null,
    @SerialName("swell_wave_height") val swellWaveHeight: Double? = null,
    @SerialName("swell_wave_period") val swellWavePeriod: Double? = null,
    @SerialName("sea_water_temperature") val seaWaterTemperature: Double? = null
)

@Serializable
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResultDto>? = null
)

@Serializable
data class OpenMeteoGeocodingResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("admin1") val admin1Province: String? = null,
    @SerialName("admin2") val admin2District: String? = null
)

interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure,uv_index",
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m,uv_index",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,uv_index_max,sunrise,sunset",
        @Query("timezone") timezone: String = "Asia/Bangkok"
    ): OpenMeteoForecastResponse

    @GET("v1/search")
    suspend fun searchGeocoding(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}

interface OpenMeteoAirQualityService {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,european_aqi,us_aqi",
        @Query("timezone") timezone: String = "Asia/Bangkok"
    ): OpenMeteoAirQualityResponse
}

interface OpenMeteoMarineService {
    @GET("v1/marine")
    suspend fun getMarine(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "wave_height,wave_direction,wave_period,swell_wave_height,swell_wave_period,sea_water_temperature",
        @Query("timezone") timezone: String = "Asia/Bangkok"
    ): OpenMeteoMarineResponse
}
