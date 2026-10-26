package com.example.thailandweather.data.repository

import com.example.thailandweather.core.database.dao.PlacesDao
import com.example.thailandweather.core.database.entity.PlaceEntity
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.Haversine
import com.example.thailandweather.core.util.WeatherUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlacesRepository(
    private val placesDao: PlacesDao
) {

    val allPlaces: Flow<List<Place>> = placesDao.getAllPlacesFlow()
        .map { entities -> entities.map { it.toDomain() } }

    val defaultPlace: Flow<Place?> = placesDao.getDefaultPlaceFlow()
        .map { it?.toDomain() }

    suspend fun getPlaceById(id: String): Place? {
        return placesDao.getPlaceById(id)?.toDomain()
    }

    suspend fun addPlace(place: Place) {
        val count = placesDao.getPlaceCount()
        val isFirst = count == 0
        val entity = PlaceEntity.fromDomain(
            place.copy(
                isDefault = if (isFirst) true else place.isDefault,
                displayOrder = count
            )
        )
        if (entity.isDefault) {
            placesDao.clearDefaultFlags()
        }
        placesDao.insertPlace(entity)
    }

    suspend fun updatePlace(place: Place) {
        if (place.isDefault) {
            placesDao.clearDefaultFlags()
        }
        placesDao.updatePlace(PlaceEntity.fromDomain(place))
    }

    suspend fun deletePlace(id: String) {
        placesDao.deletePlaceById(id)
        val remaining = placesDao.getAllPlaces()
        if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
            placesDao.setDefaultPlace(remaining.first().id)
        }
    }

    suspend fun setDefaultPlace(id: String) {
        placesDao.clearDefaultFlags()
        placesDao.setDefaultPlace(id)
    }

    suspend fun reorderPlaces(places: List<Place>) {
        val updated = places.mapIndexed { index, place ->
            PlaceEntity.fromDomain(place.copy(displayOrder = index))
        }
        placesDao.insertPlaces(updated)
    }

    suspend fun initializeSeedPlacesIfNeeded() {
        if (placesDao.getPlaceCount() == 0) {
            placesDao.insertPlaces(WeatherUtils.DEFAULT_SEED_PLACES.map { PlaceEntity.fromDomain(it) })
        }
    }

    fun getProximityClusters(radiusKm: Double): Flow<List<Haversine.PlaceCluster>> {
        return allPlaces.map { places ->
            Haversine.clusterPlaces(places, radiusKm)
        }
    }
}
