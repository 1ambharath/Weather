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

    @Test
    fun testHourlyDateAndDayFormatting() {
        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val calNow = java.util.Calendar.getInstance(tz).apply {
            set(2026, java.util.Calendar.NOVEMBER, 2, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nowMillis = calNow.timeInMillis

        // Same day (Today)
        assertEquals("Today", WeatherUtils.formatHourlyDay(nowMillis + 3600_000L, nowMillis))
        assertEquals("2 Nov", WeatherUtils.formatHourlyDateOnly(nowMillis))

        // Next day (Tomorrow)
        val tomorrowMillis = nowMillis + 24 * 3600_000L
        assertEquals("Tomorrow", WeatherUtils.formatHourlyDay(tomorrowMillis, nowMillis))
        assertEquals("3 Nov", WeatherUtils.formatHourlyDateOnly(tomorrowMillis))

        // Day after tomorrow
        val dayAfterTomorrowMillis = nowMillis + 48 * 3600_000L
        assertEquals("Wed", WeatherUtils.formatHourlyDay(dayAfterTomorrowMillis, nowMillis))

        // Hourly range formatting
        assertEquals("Today → Tomorrow", WeatherUtils.formatHourlyRange(nowMillis, tomorrowMillis, nowMillis))
        assertEquals("Today", WeatherUtils.formatHourlyRange(nowMillis, nowMillis + 2 * 3600_000L, nowMillis))
    }
}
