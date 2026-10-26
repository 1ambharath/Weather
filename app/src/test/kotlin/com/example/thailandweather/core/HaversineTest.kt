package com.example.thailandweather.core

import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.Haversine
import org.junit.Assert.*
import org.junit.Test

class HaversineTest {

    @Test
    fun testBangkokToChiangMaiDistance() {
        // Bangkok: 13.7563, 100.5018
        // Chiang Mai: 18.7883, 98.9853
        val distance = Haversine.calculateDistanceKm(13.7563, 100.5018, 18.7883, 98.9853)
        // Expected distance is approximately 580 - 600 km
        assertTrue("Distance should be between 580 and 600 km, was: $distance", distance in 580.0..600.0)
    }

    @Test
    fun testBangkokToPattayaDistance() {
        // Bangkok: 13.7563, 100.5018
        // Pattaya: 12.9276, 100.8771
        val distance = Haversine.calculateDistanceKm(13.7563, 100.5018, 12.9276, 100.8771)
        // Distance should be approximately 95 - 110 km
        assertTrue("Distance should be between 95 and 110 km, was: $distance", distance in 95.0..115.0)
    }

    @Test
    fun testIdenticalCoordinatesReturnZero() {
        val distance = Haversine.calculateDistanceKm(13.7563, 100.5018, 13.7563, 100.5018)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testProximityClustering() {
        val bkkCenter = Place("bkk_1", "Grand Palace", null, "Bangkok", null, 13.7500, 100.4913)
        val bkkSukhumvit = Place("bkk_2", "Asok Sukhumvit", null, "Bangkok", null, 13.7370, 100.5604) // ~8 km away
        val chiangMai = Place("cmi_1", "Nimman", null, "Chiang Mai", null, 18.7961, 98.9691) // ~580 km away

        val places = listOf(bkkCenter, bkkSukhumvit, chiangMai)

        // With 15 km radius, the two Bangkok spots should cluster together, while Chiang Mai is separate
        val clusters = Haversine.clusterPlaces(places, radiusKm = 15.0)
        assertEquals(2, clusters.size)

        val bkkCluster = clusters.find { it.primaryPlace.id == "bkk_1" }
        assertNotNull(bkkCluster)
        assertEquals(1, bkkCluster!!.nearbyPlaces.size)
        assertEquals("bkk_2", bkkCluster.nearbyPlaces.first().id)
        assertEquals(2, bkkCluster.totalCount)

        val cmiCluster = clusters.find { it.primaryPlace.id == "cmi_1" }
        assertNotNull(cmiCluster)
        assertEquals(0, cmiCluster!!.nearbyPlaces.size)
        assertEquals(1, cmiCluster.totalCount)
    }

    @Test
    fun testLargeSavedPlacesListClusteringPerformance() {
        // Test scalability with 500+ coordinates across Thailand
        val places = (1..500).map { i ->
            val lat = 13.0 + (i % 50) * 0.05
            val lon = 100.0 + (i % 40) * 0.05
            Place("place_$i", "Spot $i", null, "Province ${(i % 10)}", null, lat, lon)
        }

        val startTime = System.currentTimeMillis()
        val clusters = Haversine.clusterPlaces(places, radiusKm = 25.0)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("Clustering 500 places should take under 500ms, took ${elapsed}ms", elapsed < 500)
        assertTrue("Clusters should be created", clusters.isNotEmpty())
    }
}
