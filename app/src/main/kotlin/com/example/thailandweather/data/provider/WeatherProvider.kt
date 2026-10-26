package com.example.thailandweather.data.provider

import com.example.thailandweather.core.model.DailyForecast
import com.example.thailandweather.core.model.HourlyForecast
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.model.WeatherSnapshot

/**
 * WeatherProvider interface allowing pluggable weather services (e.g. Open-Meteo, TMD).
 */
interface WeatherProvider {
    val providerId: String
    val providerName: String
    val attributionNotice: String
    val termsUrl: String

    suspend fun fetchWeatherSnapshot(
        place: Place,
        includeAirQuality: Boolean = true,
        includeMarine: Boolean = true
    ): Result<WeatherSnapshot>

    suspend fun fetchHourlyForecast(place: Place): Result<List<HourlyForecast>>

    suspend fun fetchDailyForecast(place: Place): Result<List<DailyForecast>>

    suspend fun searchLocations(query: String): Result<List<Place>>
}
