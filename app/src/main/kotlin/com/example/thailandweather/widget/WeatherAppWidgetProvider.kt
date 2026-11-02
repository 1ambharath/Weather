package com.example.thailandweather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class WeatherAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            WeatherWidgetManager.updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            when (intent.action) {
                WeatherWidgetManager.ACTION_PREV_LOCATION -> {
                    WeatherWidgetManager.cycleLocation(context, appWidgetId, -1)
                }
                WeatherWidgetManager.ACTION_NEXT_LOCATION -> {
                    WeatherWidgetManager.cycleLocation(context, appWidgetId, 1)
                }
                WeatherWidgetManager.ACTION_REFRESH_WIDGET -> {
                    WeatherWidgetManager.refreshCurrentLocation(context, appWidgetId)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            WeatherWidgetManager.cleanupWidget(context, appWidgetId)
        }
    }
}
