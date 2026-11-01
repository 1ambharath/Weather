package com.example.thailandweather

import android.app.Application
import com.example.thailandweather.core.database.AppDatabase
import com.example.thailandweather.core.datastore.UserPreferencesDataStore
import com.example.thailandweather.data.provider.OpenMeteoWeatherProvider
import com.example.thailandweather.data.provider.WeatherProvider
import com.example.thailandweather.data.repository.AlertsRepository
import com.example.thailandweather.data.repository.NewsRepository
import com.example.thailandweather.data.repository.PlacesRepository
import com.example.thailandweather.data.repository.WeatherRepository
import com.example.thailandweather.notification.NotificationHelper
import com.example.thailandweather.worker.WeatherSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WeatherApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesDataStore: UserPreferencesDataStore
        private set

    lateinit var weatherProvider: WeatherProvider
        private set

    lateinit var placesRepository: PlacesRepository
        private set

    lateinit var weatherRepository: WeatherRepository
        private set

    lateinit var alertsRepository: AlertsRepository
        private set

    lateinit var newsRepository: NewsRepository
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    lateinit var secureKeyStorage: com.example.thailandweather.core.security.SecureKeyStorage
        private set

    lateinit var groqClient: com.example.thailandweather.data.remote.GroqClient
        private set

    lateinit var aiRepository: com.example.thailandweather.data.repository.AiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferencesDataStore = UserPreferencesDataStore(this)
        weatherProvider = OpenMeteoWeatherProvider()
        placesRepository = PlacesRepository(database.placesDao())
        weatherRepository = WeatherRepository(database.weatherCacheDao(), weatherProvider)
        alertsRepository = AlertsRepository(database.alertsDao())
        newsRepository = NewsRepository(database.newsDao())
        notificationHelper = NotificationHelper(this)

        secureKeyStorage = com.example.thailandweather.core.security.KeystoreEncryptedKeyStorage(this)
        groqClient = com.example.thailandweather.data.remote.GroqClient()
        aiRepository = com.example.thailandweather.data.repository.DefaultAiRepository(
            briefingDao = database.aiBriefingDao(),
            secureKeyStorage = secureKeyStorage,
            groqClient = groqClient,
            preferencesDataStore = preferencesDataStore,
            placesRepository = placesRepository,
            weatherRepository = weatherRepository,
            alertsRepository = alertsRepository,
            newsRepository = newsRepository
        )

        // Seed initial curated Thai destinations and schedule sync
        applicationScope.launch {
            placesRepository.initializeSeedPlacesIfNeeded()
            alertsRepository.refreshOfficialAlerts()
            newsRepository.refreshArticles()
            WeatherSyncWorker.schedulePeriodicSync(this@WeatherApplication, intervalMinutes = 30)
        }
    }

    companion object {
        lateinit var instance: WeatherApplication
            private set
    }
}
