package com.example.thailandweather.worker

import android.content.Context
import androidx.work.*
import com.example.thailandweather.core.database.AppDatabase
import com.example.thailandweather.core.datastore.UserPreferencesDataStore
import com.example.thailandweather.data.provider.OpenMeteoWeatherProvider
import com.example.thailandweather.data.repository.AlertsRepository
import com.example.thailandweather.data.repository.NewsRepository
import com.example.thailandweather.data.repository.PlacesRepository
import com.example.thailandweather.data.repository.WeatherRepository
import com.example.thailandweather.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val preferencesDataStore = UserPreferencesDataStore(applicationContext)
            val prefs = preferencesDataStore.preferencesFlow.first()

            val placesRepo = PlacesRepository(db.placesDao())
            val weatherRepo = WeatherRepository(db.weatherCacheDao(), OpenMeteoWeatherProvider())
            val alertsRepo = AlertsRepository(db.alertsDao())
            val newsRepo = NewsRepository(db.newsDao())
            val notificationHelper = NotificationHelper(applicationContext)

            // 1. Refresh official warnings
            val officialAlerts = alertsRepo.refreshOfficialAlerts()
            if (prefs.notificationsEnabled && prefs.notifyOfficialWarnings) {
                officialAlerts.filter { it.severity.level >= com.example.thailandweather.core.model.AlertSeverity.WARNING.level }
                    .forEach { notificationHelper.showOfficialWarning(it) }
            }

            // 2. Refresh news
            newsRepo.refreshArticles()

            // 3. Sync default place weather and evaluate forecast risks
            val defaultPlace = placesRepo.defaultPlace.first()
            if (defaultPlace != null && defaultPlace.alertEnabled) {
                val weatherStream = weatherRepo.getWeatherStream(defaultPlace, forceRefresh = true)
                val snapshot = weatherStream.first()

                if (prefs.notificationsEnabled && prefs.notifyForecastRisks) {
                    val risks = alertsRepo.evaluateForecastRisks(defaultPlace, snapshot)
                    risks.forEach { notificationHelper.showForecastRisk(it) }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "ThailandWeatherSyncWorker"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Long = 30) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            ).setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
