package com.example.thailandweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.thailandweather.MainActivity
import com.example.thailandweather.R
import com.example.thailandweather.WeatherApplication
import com.example.thailandweather.core.database.AppDatabase
import com.example.thailandweather.core.model.HourlyForecast
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.model.WeatherSnapshot
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.data.provider.OpenMeteoWeatherProvider
import com.example.thailandweather.data.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object WeatherWidgetManager {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val PREFS_NAME = "thailand_weather_widget_prefs"
    private const val KEY_PREFIX_INDEX = "widget_location_idx_"

    const val ACTION_PREV_LOCATION = "com.example.thailandweather.widget.ACTION_PREV_LOCATION"
    const val ACTION_NEXT_LOCATION = "com.example.thailandweather.widget.ACTION_NEXT_LOCATION"
    const val ACTION_REFRESH_WIDGET = "com.example.thailandweather.widget.ACTION_REFRESH_WIDGET"
    const val EXTRA_PLACE_ID = "extra_place_id"

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        widgetScope.launch {
            renderWidget(context, appWidgetManager, appWidgetId)
        }
    }

    fun cycleLocation(context: Context, appWidgetId: Int, direction: Int) {
        widgetScope.launch {
            val places = getSavedPlaces(context)
            if (places.isNotEmpty()) {
                val currentIndex = getWidgetLocationIndex(context, appWidgetId, places.size)
                val newIndex = (currentIndex + direction + places.size) % places.size
                saveWidgetLocationIndex(context, appWidgetId, newIndex)
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            renderWidget(context, appWidgetManager, appWidgetId)
        }
    }

    fun refreshCurrentLocation(context: Context, appWidgetId: Int) {
        widgetScope.launch {
            val places = getSavedPlaces(context)
            if (places.isNotEmpty()) {
                val currentIndex = getWidgetLocationIndex(context, appWidgetId, places.size)
                val activePlace = places[currentIndex]
                val weatherRepo = getWeatherRepository(context)
                weatherRepo.refreshWeather(activePlace)
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            renderWidget(context, appWidgetManager, appWidgetId)
        }
    }

    fun updateAllWidgets(context: Context) {
        widgetScope.launch {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WeatherAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            for (id in appWidgetIds) {
                renderWidget(context, appWidgetManager, id)
            }
        }
    }

    fun cleanupWidget(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_PREFIX_INDEX$appWidgetId").apply()
    }

    private suspend fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val places = getSavedPlaces(context)
            if (places.isEmpty()) {
                renderEmptyWidget(context, appWidgetManager, appWidgetId)
                return@withContext
            }

            val currentIndex = getWidgetLocationIndex(context, appWidgetId, places.size)
            val activePlace = places[currentIndex]
            val weatherRepo = getWeatherRepository(context)

            // Attempt to get cached weather first for 0ms response; fallback to live stream or synthesis
            var snapshot: WeatherSnapshot? = weatherRepo.getCachedWeatherSnapshot(activePlace.id)
            if (snapshot == null) {
                val liveResult = OpenMeteoWeatherProvider().fetchWeatherSnapshot(activePlace)
                snapshot = liveResult.getOrNull() ?: weatherRepo.createFallbackSnapshot(activePlace)
            }

            var hourly: List<HourlyForecast> = weatherRepo.getHourlyForecast(activePlace)
            if (hourly.isEmpty()) {
                hourly = weatherRepo.createFallbackHourly(activePlace)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_compact_weather_4x2)

            // 1. Location Header & Index
            val locationHeader = "${activePlace.name} (${currentIndex + 1}/${places.size})"
            views.setTextViewText(R.id.tv_widget_location_title, locationHeader)

            // 2. Current Telemetry
            val tempText = "${Math.round(snapshot.temperatureCelsius)}°"
            views.setTextViewText(R.id.tv_widget_temp, tempText)
            views.setImageViewResource(R.id.iv_widget_condition_icon, getWeatherIconDrawable(snapshot.weatherCode))
            views.setTextViewText(R.id.tv_widget_condition_desc, snapshot.weatherDescription)

            val aqiValue = snapshot.airQuality?.aqiUs ?: 38
            val rainPop = snapshot.precipitationProbability
            val feelsLike = Math.round(snapshot.apparentTemperatureCelsius)
            val submetrics = "Feels $feelsLike° · AQI $aqiValue · 🌧️ $rainPop%"
            views.setTextViewText(R.id.tv_widget_submetrics, submetrics)

            // 3. Hourly Outlook Cards (4 Slots)
            bindHourlySlot(views, hourly.getOrNull(0), R.id.tv_hourly_time_1, R.id.iv_hourly_icon_1, R.id.tv_hourly_temp_1, R.id.tv_hourly_pop_1, isFirst = true)
            bindHourlySlot(views, hourly.getOrNull(1), R.id.tv_hourly_time_2, R.id.iv_hourly_icon_2, R.id.tv_hourly_temp_2, R.id.tv_hourly_pop_2, isFirst = false)
            bindHourlySlot(views, hourly.getOrNull(2), R.id.tv_hourly_time_3, R.id.iv_hourly_icon_3, R.id.tv_hourly_temp_3, R.id.tv_hourly_pop_3, isFirst = false)
            bindHourlySlot(views, hourly.getOrNull(3), R.id.tv_hourly_time_4, R.id.iv_hourly_icon_4, R.id.tv_hourly_temp_4, R.id.tv_hourly_pop_4, isFirst = false)

            // 4. Pending Intents (Deep link & cycling controls)
            val rootIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_PLACE_ID, activePlace.id)
            }
            val rootPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)

            // Prev Button
            val prevIntent = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = ACTION_PREV_LOCATION
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_prev, prevPendingIntent)

            // Next Button
            val nextIntent = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = ACTION_NEXT_LOCATION
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 2,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_next, nextPendingIntent)

            // Refresh Button
            val refreshIntent = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 3,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bindHourlySlot(
        views: RemoteViews,
        forecast: HourlyForecast?,
        timeRes: Int,
        iconRes: Int,
        tempRes: Int,
        popRes: Int,
        isFirst: Boolean
    ) {
        if (forecast != null) {
            val timeLabel = if (isFirst) "NOW" else WeatherUtils.formatTime(forecast.time)
            views.setTextViewText(timeRes, timeLabel)
            views.setImageViewResource(iconRes, getWeatherIconDrawable(forecast.weatherCode))
            views.setTextViewText(tempRes, "${Math.round(forecast.temperatureCelsius)}°")
            views.setTextViewText(popRes, "${forecast.precipitationProbability}%")
        } else {
            views.setTextViewText(timeRes, "--:--")
            views.setImageViewResource(iconRes, R.drawable.ic_wx_partly_cloudy)
            views.setTextViewText(tempRes, "--°")
            views.setTextViewText(popRes, "--%")
        }
    }

    private fun renderEmptyWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_weather_4x2)
        views.setTextViewText(R.id.tv_widget_location_title, "Thailand Weather")
        views.setTextViewText(R.id.tv_widget_temp, "--°")
        views.setTextViewText(R.id.tv_widget_condition_desc, "No saved places")
        views.setTextViewText(R.id.tv_widget_submetrics, "Open app to add destinations")

        val rootIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun getSavedPlaces(context: Context): List<Place> {
        return try {
            val database = AppDatabase.getInstance(context)
            val places = database.placesDao().getAllPlaces().map { it.toDomain() }
            if (places.isNotEmpty()) places else WeatherUtils.DEFAULT_SEED_PLACES
        } catch (_: Exception) {
            WeatherUtils.DEFAULT_SEED_PLACES
        }
    }

    private fun getWeatherRepository(context: Context): WeatherRepository {
        return try {
            WeatherApplication.instance.weatherRepository
        } catch (_: Exception) {
            val db = AppDatabase.getInstance(context)
            WeatherRepository(db.weatherCacheDao(), OpenMeteoWeatherProvider())
        }
    }

    private fun getWidgetLocationIndex(context: Context, appWidgetId: Int, placeCount: Int): Int {
        if (placeCount <= 0) return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getInt("$KEY_PREFIX_INDEX$appWidgetId", 0)
        return if (raw in 0 until placeCount) raw else 0
    }

    private fun saveWidgetLocationIndex(context: Context, appWidgetId: Int, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("$KEY_PREFIX_INDEX$appWidgetId", index).apply()
    }

    fun getWeatherIconDrawable(code: Int): Int = when (code) {
        0 -> R.drawable.ic_wx_sunny
        1, 2 -> R.drawable.ic_wx_partly_cloudy
        3 -> R.drawable.ic_wx_cloudy
        45, 48 -> R.drawable.ic_wx_fog
        51, 53, 55, 61, 63 -> R.drawable.ic_wx_rain
        65, 80, 81, 82 -> R.drawable.ic_wx_rain
        95, 96, 99 -> R.drawable.ic_wx_thunderstorm
        else -> R.drawable.ic_wx_partly_cloudy
    }
}
