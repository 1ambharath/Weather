package com.example.thailandweather.core.util

import com.example.thailandweather.core.model.Place
import kotlin.math.*

object Haversine {
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the great-circle distance between two geographic coordinates in kilometers.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Determines if two places are within a specific proximity radius.
     */
    fun isWithinRadius(p1: Place, p2: Place, radiusKm: Double): Boolean {
        return calculateDistanceKm(p1.latitude, p1.longitude, p2.latitude, p2.longitude) <= radiusKm
    }

    /**
     * Cluster representing nearby places within the configured radius.
     */
    data class PlaceCluster(
        val clusterId: String,
        val primaryPlace: Place,
        val nearbyPlaces: List<Place>,
        val radiusKm: Double
    ) {
        val totalCount: Int get() = 1 + nearbyPlaces.size
    }

    /**
     * Groups places into automatic geographic clusters based on radius.
     * Manual custom groups remain preserved separately.
     */
    fun clusterPlaces(
        places: List<Place>,
        radiusKm: Double
    ): List<PlaceCluster> {
        val visited = mutableSetOf<String>()
        val clusters = mutableListOf<PlaceCluster>()

        for (place in places) {
            if (place.id in visited) continue
            visited.add(place.id)

            val nearby = mutableListOf<Place>()
            for (other in places) {
                if (other.id !in visited && isWithinRadius(place, other, radiusKm)) {
                    nearby.add(other)
                    visited.add(other.id)
                }
            }
            clusters.add(
                PlaceCluster(
                    clusterId = "cluster_${place.id}",
                    primaryPlace = place,
                    nearbyPlaces = nearby,
                    radiusKm = radiusKm
                )
            )
        }
        return clusters
    }
}
