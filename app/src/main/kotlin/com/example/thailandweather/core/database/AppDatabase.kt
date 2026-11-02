package com.example.thailandweather.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.thailandweather.core.database.dao.AlertsDao
import com.example.thailandweather.core.database.dao.NewsDao
import com.example.thailandweather.core.database.dao.PlacesDao
import com.example.thailandweather.core.database.dao.WeatherCacheDao
import com.example.thailandweather.core.database.entity.AlertEntity
import com.example.thailandweather.core.database.entity.ArticleEntity
import com.example.thailandweather.core.database.entity.PlaceEntity
import com.example.thailandweather.core.database.entity.WeatherCacheEntity

@Database(
    entities = [
        PlaceEntity::class,
        WeatherCacheEntity::class,
        AlertEntity::class,
        ArticleEntity::class,
        com.example.thailandweather.core.database.entity.AiBriefingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placesDao(): PlacesDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun alertsDao(): AlertsDao
    abstract fun newsDao(): NewsDao
    abstract fun aiBriefingDao(): com.example.thailandweather.core.database.dao.AiBriefingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thailand_weather.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
