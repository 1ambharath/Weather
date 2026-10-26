package com.example.thailandweather.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.OfficialAlert
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAlerts: () -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToPlaces: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLocationMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showLocationMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.selectedPlace?.let { "${it.name}, ${it.province}" } ?: "Thailand",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Change place"
                        )
                    }

                    DropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false }
                    ) {
                        uiState.allPlaces.forEach { place ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = place.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${place.province} • ${place.customGroup ?: "General"}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectPlace(place)
                                    showLocationMenu = false
                                },
                                leadingIcon = {
                                    if (place.id == uiState.selectedPlace?.id) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh weather"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.currentSnapshot == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    uiState.currentSnapshot?.let { snapshot ->
                        StaleDataBanner(
                            isStale = snapshot.isStale,
                            dataSource = snapshot.dataSource
                        )
                    }
                }

                // High priority alert card
                if (uiState.highPriorityAlerts.isNotEmpty()) {
                    item {
                        val alert = uiState.highPriorityAlerts.first()
                        OfficialAlertBanner(
                            alert = alert,
                            onClick = onNavigateToAlerts
                        )
                    }
                }

                // Current Weather Hero
                item {
                    uiState.currentSnapshot?.let { snapshot ->
                        CurrentWeatherHeroCard(
                            snapshot = snapshot,
                            place = uiState.selectedPlace,
                            daily = uiState.dailyForecast.firstOrNull()
                        )
                    }
                }

                // Hourly Forecast Carousel
                item {
                    HourlyForecastSection(
                        hourly = uiState.hourlyForecast,
                        isCelsius = true
                    )
                }

                // Air Quality Section
                item {
                    AirQualityCard(airQuality = uiState.currentSnapshot?.airQuality)
                }

                // Marine Conditions (if coastal)
                if (uiState.selectedPlace?.isCoastal == true || uiState.currentSnapshot?.marine != null) {
                    item {
                        MarineConditionsCard(marine = uiState.currentSnapshot?.marine)
                    }
                }

                // Saved Places Weather Comparison
                if (uiState.allPlaces.size > 1) {
                    item {
                        SavedPlacesComparisonSection(
                            places = uiState.allPlaces,
                            snapshots = uiState.comparisonSnapshots,
                            onPlaceClick = { viewModel.selectPlace(it) },
                            onManageClick = onNavigateToPlaces
                        )
                    }
                }

                // Recent News Teaser
                if (uiState.recentNews.isNotEmpty()) {
                    item {
                        RecentNewsSection(
                            articles = uiState.recentNews,
                            onSeeAll = onNavigateToNews
                        )
                    }
                }

                // Source Attribution & Metadata
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Weather data by Open-Meteo.com (CC-BY 4.0)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Alerts & Radar provided by TMD & DDPM Thailand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentWeatherHeroCard(
    snapshot: com.example.thailandweather.core.model.WeatherSnapshot,
    place: Place?,
    daily: com.example.thailandweather.core.model.DailyForecast?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = place?.name ?: snapshot.placeName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        place?.thaiName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(
                        imageVector = getWeatherIcon(snapshot.weatherCode),
                        contentDescription = snapshot.weatherDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${Math.round(snapshot.temperatureCelsius)}°",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = snapshot.weatherDescription,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Feels like ${Math.round(snapshot.apparentTemperatureCelsius)}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                daily?.let {
                    Text(
                        text = "High: ${it.maxTempCelsius.toInt()}°C  •  Low: ${it.minTempCelsius.toInt()}°C  •  Rain chance: ${it.precipitationProbabilityMax}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Weather indicators grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IndicatorItem(
                        icon = Icons.Default.WaterDrop,
                        label = "Humidity",
                        value = "${snapshot.relativeHumidity}%"
                    )
                    IndicatorItem(
                        icon = Icons.Default.Air,
                        label = "Wind",
                        value = "${snapshot.windSpeedKmh.toInt()} km/h"
                    )
                    IndicatorItem(
                        icon = Icons.Default.WbSunny,
                        label = "UV Index",
                        value = "${snapshot.uvIndex.toInt()}"
                    )
                    IndicatorItem(
                        icon = Icons.Default.Speed,
                        label = "Pressure",
                        value = "${snapshot.surfacePressureHpa?.toInt() ?: 1012} hPa"
                    )
                }
            }
        }
    }
}

@Composable
fun IndicatorItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun OfficialAlertBanner(
    alert: OfficialAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alertColor = Color(alert.severity.hexColor)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, alertColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = alertColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                HazardBadge(severity = alert.severity, isOfficial = true)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Text(
                    text = "Source: ${alert.source}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = alertColor
            )
        }
    }
}

@Composable
fun SavedPlacesComparisonSection(
    places: List<Place>,
    snapshots: Map<String, com.example.thailandweather.core.model.WeatherSnapshot>,
    onPlaceClick: (Place) -> Unit,
    onManageClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Places Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onManageClick) {
                Text(text = "Manage")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(places) { place ->
                val snap = snapshots[place.id]
                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onPlaceClick(place) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = place.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = place.province,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = snap?.let { "${Math.round(it.temperatureCelsius)}°C" } ?: "--",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            snap?.let {
                                Icon(
                                    imageVector = getWeatherIcon(it.weatherCode),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        snap?.airQuality?.let { aq ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PM2.5: ${aq.pm25.toInt()}",
                                fontSize = 11.sp,
                                color = Color(aq.category.hexColor),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentNewsSection(
    articles: List<com.example.thailandweather.core.model.Article>,
    onSeeAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thailand Weather News",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSeeAll) {
                Text(text = "See All")
            }
        }

        articles.take(2).forEach { article ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = article.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = article.publisher,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
