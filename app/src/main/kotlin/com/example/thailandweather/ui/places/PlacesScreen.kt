package com.example.thailandweather.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.Haversine
import com.example.thailandweather.ui.components.getWeatherIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel,
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Places & Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog(true) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Place")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // View Mode Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.viewMode == PlacesViewMode.COMPARISON_LIST,
                    onClick = { viewModel.setViewMode(PlacesViewMode.COMPARISON_LIST) },
                    label = { Text("All Places (${uiState.places.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = uiState.viewMode == PlacesViewMode.PROXIMITY_CLUSTERS,
                    onClick = { viewModel.setViewMode(PlacesViewMode.PROXIMITY_CLUSTERS) },
                    label = { Text("Proximity Clusters", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = uiState.viewMode == PlacesViewMode.CUSTOM_GROUPS,
                    onClick = { viewModel.setViewMode(PlacesViewMode.CUSTOM_GROUPS) },
                    label = { Text("Custom Groups", fontSize = 12.sp) }
                )
            }

            // Proximity Radius Selector (when in cluster mode)
            if (uiState.viewMode == PlacesViewMode.PROXIMITY_CLUSTERS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cluster Radius:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf(5.0, 10.0, 25.0, 50.0).forEach { radius ->
                        val isSelected = uiState.selectedRadiusKm == radius
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setProximityRadius(radius) },
                            label = { Text("${radius.toInt()} km", fontSize = 11.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content by ViewMode
            when (uiState.viewMode) {
                PlacesViewMode.COMPARISON_LIST -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.places) { place ->
                            PlaceComparisonCard(
                                place = place,
                                snapshot = uiState.snapshots[place.id],
                                defaultPlace = uiState.places.firstOrNull { it.isDefault },
                                onClick = { onPlaceSelected(place) },
                                onSetDefault = { viewModel.setDefault(place.id) },
                                onToggleAlerts = { viewModel.toggleAlerts(place) },
                                onDelete = { viewModel.deletePlace(place.id) }
                            )
                        }
                    }
                }
                PlacesViewMode.PROXIMITY_CLUSTERS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.clusters) { cluster ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cluster: ${cluster.primaryPlace.province} (${cluster.totalCount} locations within ${cluster.radiusKm.toInt()} km)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PlaceComparisonCard(
                                        place = cluster.primaryPlace,
                                        snapshot = uiState.snapshots[cluster.primaryPlace.id],
                                        defaultPlace = uiState.places.firstOrNull { it.isDefault },
                                        onClick = { onPlaceSelected(cluster.primaryPlace) },
                                        onSetDefault = { viewModel.setDefault(cluster.primaryPlace.id) },
                                        onToggleAlerts = { viewModel.toggleAlerts(cluster.primaryPlace) },
                                        onDelete = { viewModel.deletePlace(cluster.primaryPlace.id) }
                                    )
                                    cluster.nearbyPlaces.forEach { member ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        PlaceComparisonCard(
                                            place = member,
                                            snapshot = uiState.snapshots[member.id],
                                            defaultPlace = uiState.places.firstOrNull { it.isDefault },
                                            onClick = { onPlaceSelected(member) },
                                            onSetDefault = { viewModel.setDefault(member.id) },
                                            onToggleAlerts = { viewModel.toggleAlerts(member) },
                                            onDelete = { viewModel.deletePlace(member.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                PlacesViewMode.CUSTOM_GROUPS -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        uiState.customGroupsMap.forEach { (groupName, placesInGroup) ->
                            item {
                                Text(
                                    text = "📂 $groupName (${placesInGroup.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(placesInGroup) { place ->
                                PlaceComparisonCard(
                                    place = place,
                                    snapshot = uiState.snapshots[place.id],
                                    defaultPlace = uiState.places.firstOrNull { it.isDefault },
                                    onClick = { onPlaceSelected(place) },
                                    onSetDefault = { viewModel.setDefault(place.id) },
                                    onToggleAlerts = { viewModel.toggleAlerts(place) },
                                    onDelete = { viewModel.deletePlace(place.id) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddingPlace) {
        AddPlaceDialog(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onAddPlace = { viewModel.addPlace(it) },
            onAddCustom = { name, prov, lat, lon, group ->
                viewModel.addCustomCoordinatePlace(name, prov, lat, lon, group)
            },
            onDismiss = { viewModel.showAddDialog(false) }
        )
    }
}

@Composable
fun PlaceComparisonCard(
    place: Place,
    snapshot: com.example.thailandweather.core.model.WeatherSnapshot?,
    defaultPlace: Place?,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onToggleAlerts: () -> Unit,
    onDelete: () -> Unit
) {
    val distanceKm = if (defaultPlace != null && defaultPlace.id != place.id) {
        Haversine.calculateDistanceKm(
            defaultPlace.latitude,
            defaultPlace.longitude,
            place.latitude,
            place.longitude
        )
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (place.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = snapshot?.let { "${Math.round(it.temperatureCelsius)}°C" } ?: "--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    snapshot?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = getWeatherIcon(it.weatherCode),
                            contentDescription = it.weatherDescription,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Text(
                text = "${place.province} • ${place.customGroup ?: "General"} ${distanceKm?.let { "• ${it.toInt()} km away" } ?: ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-metrics (Air quality, Marine, Feels-like)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot?.airQuality?.let { aq ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(aq.category.hexColor).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PM2.5: ${aq.pm25.toInt()} (${aq.category.label})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(aq.category.hexColor),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (place.isCoastal && snapshot?.marine != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00B0FF).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Wave: ${String.format("%.1f", snapshot.marine.waveHeightMeters)}m",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0091EA),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onToggleAlerts,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (place.alertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Alerts",
                            tint = if (place.alertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (!place.isDefault) {
                        IconButton(
                            onClick = onSetDefault,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StarBorder,
                                contentDescription = "Set default",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddPlaceDialog(
    searchQuery: String,
    searchResults: List<Place>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onAddPlace: (Place) -> Unit,
    onAddCustom: (String, String, Double, Double, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var customProvince by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf("") }
    var customLon by remember { mutableStateOf("") }
    var customGroup by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Thailand Place", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Search") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Coordinates") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        label = { Text("Search city, island or landmark") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(searchResults) { result ->
                                ListItem(
                                    headlineContent = { Text(result.name, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("${result.province} (${String.format("%.2f", result.latitude)}, ${String.format("%.2f", result.longitude)})") },
                                    modifier = Modifier.clickable { onAddPlace(result) }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Location Name (e.g. Phi Phi Viewpoint)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customProvince,
                        onValueChange = { customProvince = it },
                        label = { Text("Province (e.g. Krabi)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customLat,
                            onValueChange = { customLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customLon,
                            onValueChange = { customLon = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customGroup,
                        onValueChange = { customGroup = it },
                        label = { Text("Custom Group (e.g. Vacation / Family)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        val lat = customLat.toDoubleOrNull() ?: 13.75
                        val lon = customLon.toDoubleOrNull() ?: 100.5
                        if (customName.isNotBlank()) {
                            onAddCustom(customName, customProvince.ifBlank { "Thailand" }, lat, lon, customGroup.ifBlank { null })
                        }
                    }
                ) {
                    Text("Add Location")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
