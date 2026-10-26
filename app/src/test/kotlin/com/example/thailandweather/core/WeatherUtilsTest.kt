package com.example.thailandweather.core

import com.example.thailandweather.core.model.AirQualityCategory
import com.example.thailandweather.core.util.WeatherUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUtilsTest {

    @Test
    fun testCelsiusToFahrenheitConversion() {
        assertEquals(32.0, WeatherUtils.celsiusToFahrenheit(0.0), 0.01)
        assertEquals(212.0, WeatherUtils.celsiusToFahrenheit(100.0), 0.01)
        assertEquals(95.0, WeatherUtils.celsiusToFahrenheit(35.0), 0.01)
        assertEquals(86.0, WeatherUtils.celsiusToFahrenheit(30.0), 0.01)
    }

    @Test
    fun testFormatTemperature() {
        assertEquals("32°C", WeatherUtils.formatTemperature(32.4, isCelsius = true))
        assertEquals("90°F", WeatherUtils.formatTemperature(32.4, isCelsius = false))
    }

    @Test
    fun testFormatWindSpeed() {
        val kmh = 20.0
        val formattedKmh = WeatherUtils.formatWindSpeed(kmh, isKmh = true)
        assertTrue("Formatted wind km/h should contain km/h", formattedKmh.contains("km/h"))

        val formattedMph = WeatherUtils.formatWindSpeed(kmh, isKmh = false)
        assertTrue("Formatted wind mph should contain mph", formattedMph.contains("mph"))
    }

    @Test
    fun testWmoWeatherDescriptions() {
        // Clear sky
        assertEquals("Clear sky", WeatherUtils.getWeatherDescription(0, isThai = false))
        assertEquals("ท้องฟ้าแจ่มใส", WeatherUtils.getWeatherDescription(0, isThai = true))

        // Rain & Storms
        assertEquals("Heavy rain", WeatherUtils.getWeatherDescription(65, isThai = false))
        assertEquals("ฝนตกหนัก", WeatherUtils.getWeatherDescription(65, isThai = true))

        assertEquals("Thunderstorm", WeatherUtils.getWeatherDescription(95, isThai = false))
        assertEquals("พายุฝนฟ้าคะนอง", WeatherUtils.getWeatherDescription(95, isThai = true))
    }

    @Test
    fun testAirQualityCategories() {
        // PCD standards
        assertEquals(AirQualityCategory.VERY_GOOD, AirQualityCategory.fromPm25(15.0))
        assertEquals(AirQualityCategory.GOOD, AirQualityCategory.fromPm25(30.0))
        assertEquals(AirQualityCategory.MODERATE, AirQualityCategory.fromPm25(45.0))
        assertEquals(AirQualityCategory.UNHEALTHY_SENSITIVE, AirQualityCategory.fromPm25(75.0))
        assertEquals(AirQualityCategory.UNHEALTHY, AirQualityCategory.fromPm25(120.0))
        assertEquals(AirQualityCategory.HAZARDOUS, AirQualityCategory.fromPm25(180.0))
    }
}
