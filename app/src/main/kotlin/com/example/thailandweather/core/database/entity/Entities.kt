package com.example.thailandweather.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thailandweather.core.model.Place

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thaiName: String?,
    val province: String,
    val district: String?,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean,
    val customGroup: String?,
    val isCoastal: Boolean,
    val alertEnabled: Boolean,
    val proximityRadiusKm: Int,
    val displayOrder: Int,
    val createdAt: Long
) {
    fun toDomain(): Place = Place(
        id = id,
        name = name,
        thaiName = thaiName,
        province = province,
        district = district,
        latitude = latitude,
        longitude = longitude,
        isDefault = isDefault,
        customGroup = customGroup,
        isCoastal = isCoastal,
        alertEnabled = alertEnabled,
        proximityRadiusKm = proximityRadiusKm,
        displayOrder = displayOrder,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(p: Place): PlaceEntity = PlaceEntity(
            id = p.id,
            name = p.name,
            thaiName = p.thaiName,
            province = p.province,
            district = p.district,
            latitude = p.latitude,
            longitude = p.longitude,
            isDefault = p.isDefault,
            customGroup = p.customGroup,
            isCoastal = p.isCoastal,
            alertEnabled = p.alertEnabled,
            proximityRadiusKm = p.proximityRadiusKm,
            displayOrder = p.displayOrder,
            createdAt = p.createdAt
        )
    }
}

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val placeId: String,
    val dataJson: String,
    val cachedAt: Long,
    val expiresAt: Long,
    val isStale: Boolean
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val alertType: String, // "OFFICIAL" or "RISK"
    val dataJson: String,
    val severityLevel: Int,
    val issuedAt: Long,
    val expiresAt: Long,
    val isActive: Boolean
)

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val publisher: String,
    val publishedAt: Long,
    val url: String,
    val category: String,
    val province: String?,
    val imageUrl: String?,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "ai_briefings")
data class AiBriefingEntity(
    @PrimaryKey val id: String = "latest_briefing",
    val title: String,
    val thailandSummary: String,
    val locationSummariesJson: String,
    val newsAndIncidents: String,
    val riskExplanationsJson: String,
    val actionableAdviceJson: String,
    val sourcesJson: String,
    val modelUsed: String,
    val generatedAt: Long
)

