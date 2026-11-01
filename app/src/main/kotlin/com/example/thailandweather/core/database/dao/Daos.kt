package com.example.thailandweather.core.database.dao

import androidx.room.*
import com.example.thailandweather.core.database.entity.AlertEntity
import com.example.thailandweather.core.database.entity.ArticleEntity
import com.example.thailandweather.core.database.entity.PlaceEntity
import com.example.thailandweather.core.database.entity.WeatherCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacesDao {
    @Query("SELECT * FROM places ORDER BY displayOrder ASC, createdAt ASC")
    fun getAllPlacesFlow(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY displayOrder ASC, createdAt ASC")
    suspend fun getAllPlaces(): List<PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    suspend fun getPlaceById(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPlace(): PlaceEntity?

    @Query("SELECT * FROM places WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPlaceFlow(): Flow<PlaceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<PlaceEntity>)

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deletePlaceById(id: String)

    @Query("UPDATE places SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE places SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultPlace(id: String)

    @Query("SELECT COUNT(*) FROM places")
    suspend fun getPlaceCount(): Int
}

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE placeId = :placeId LIMIT 1")
    suspend fun getCachedWeather(placeId: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache WHERE placeId = :placeId LIMIT 1")
    fun getCachedWeatherFlow(placeId: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache")
    suspend fun getAllCached(): List<WeatherCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM weather_cache WHERE placeId = :placeId")
    suspend fun deleteByPlaceId(placeId: String)
}

@Dao
interface AlertsDao {
    @Query("SELECT * FROM alerts WHERE isActive = 1 ORDER BY severityLevel DESC, issuedAt DESC")
    fun getAllActiveAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE alertType = 'OFFICIAL' AND isActive = 1 ORDER BY severityLevel DESC")
    fun getOfficialAlertsFlow(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE alertType = 'RISK' AND isActive = 1 ORDER BY severityLevel DESC")
    fun getForecastRisksFlow(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("DELETE FROM alerts WHERE expiresAt < :now")
    suspend fun deleteExpiredAlerts(now: Long)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticlesFlow(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun getBookmarkedArticlesFlow(): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, isBookmarked: Boolean)

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND publishedAt < :cutoff")
    suspend fun deleteOldArticles(cutoff: Long)
}

@Dao
interface AiBriefingDao {
    @Query("SELECT * FROM ai_briefings WHERE id = :id LIMIT 1")
    fun getLatestBriefingFlow(id: String = "latest_briefing"): Flow<com.example.thailandweather.core.database.entity.AiBriefingEntity?>

    @Query("SELECT * FROM ai_briefings WHERE id = :id LIMIT 1")
    suspend fun getLatestBriefing(id: String = "latest_briefing"): com.example.thailandweather.core.database.entity.AiBriefingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBriefing(briefing: com.example.thailandweather.core.database.entity.AiBriefingEntity)

    @Query("DELETE FROM ai_briefings")
    suspend fun clearBriefings()
}
