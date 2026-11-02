package com.example.thailandweather.widget

import com.example.thailandweather.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherWidgetManagerTest {

    @Test
    fun testWeatherIconDrawableMapping() {
        assertEquals(R.drawable.ic_wx_sunny, WeatherWidgetManager.getWeatherIconDrawable(0))
        assertEquals(R.drawable.ic_wx_partly_cloudy, WeatherWidgetManager.getWeatherIconDrawable(1))
        assertEquals(R.drawable.ic_wx_partly_cloudy, WeatherWidgetManager.getWeatherIconDrawable(2))
        assertEquals(R.drawable.ic_wx_cloudy, WeatherWidgetManager.getWeatherIconDrawable(3))
        assertEquals(R.drawable.ic_wx_fog, WeatherWidgetManager.getWeatherIconDrawable(45))
        assertEquals(R.drawable.ic_wx_fog, WeatherWidgetManager.getWeatherIconDrawable(48))
        assertEquals(R.drawable.ic_wx_rain, WeatherWidgetManager.getWeatherIconDrawable(61))
        assertEquals(R.drawable.ic_wx_rain, WeatherWidgetManager.getWeatherIconDrawable(80))
        assertEquals(R.drawable.ic_wx_thunderstorm, WeatherWidgetManager.getWeatherIconDrawable(95))
        assertEquals(R.drawable.ic_wx_thunderstorm, WeatherWidgetManager.getWeatherIconDrawable(99))
    }

    @Test
    fun testLocationIndexCyclingBounds() {
        val totalPlaces = 3

        // Forward cycling from 0 -> 1 -> 2 -> 0
        var current = 0
        current = (current + 1) % totalPlaces
        assertEquals(1, current)

        current = (current + 1) % totalPlaces
        assertEquals(2, current)

        current = (current + 1) % totalPlaces
        assertEquals(0, current)

        // Backward cycling from 0 -> 2 -> 1 -> 0
        current = (current - 1 + totalPlaces) % totalPlaces
        assertEquals(2, current)

        current = (current - 1 + totalPlaces) % totalPlaces
        assertEquals(1, current)

        current = (current - 1 + totalPlaces) % totalPlaces
        assertEquals(0, current)
    }
}
