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
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    viewModel: PlacesViewModel,
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Saved Places & Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog(true) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Place",
                            tint = NomadKitPalette.PrimarySand
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
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
                PlacesViewMode.values().forEach { mode ->
                    val isSelected = uiState.viewMode == mode
                    val label = when (mode) {
                        PlacesViewMode.COMPARISON_LIST -> "All Places (${uiState.places.size})"
                        PlacesViewMode.PROXIMITY_CLUSTERS -> "Proximity Clusters"
                        PlacesViewMode.CUSTOM_GROUPS -> "Custom Groups"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setViewMode(mode) },
                        shape = RoundedCornerShape(percent = 50),
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DribbblePalette.Primary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NomadKitPalette.Outline,
                            selectedBorderColor = DribbblePalette.Primary
                        )
                    )
                }
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
                            shape = RoundedCornerShape(percent = 50),
                            label = { Text("${radius.toInt()} km", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DribbblePalette.Primary,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = NomadKitPalette.Outline,
                                selectedBorderColor = DribbblePalette.Primary
                            ),
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
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.OutlineVariant)
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
                                    Spacer(modifier = Modifier.height(10.dp))
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
                                        Spacer(modifier = Modifier.height(8.dp))
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
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NomadKitPalette.PrimarySandLight,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "📂 $groupName (${placesInGroup.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = NomadKitPalette.PrimarySandDeep,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
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
                            shape = RoundedCornerShape(percent = 50),
                            color = DribbblePalette.NeutralRose
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DribbblePalette.Accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                            tint = DribbblePalette.Accent,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-metrics (Air quality, Marine, Feels-like)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot?.airQuality?.let { aq ->
                        val aqColor = Color(aq.category.hexColor)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = aqColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, aqColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "PM2.5: ${aq.pm25.toInt()} (${aq.category.label})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = aqColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (place.isCoastal && snapshot?.marine != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NomadKitPalette.SecondaryOceanLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.SecondaryOcean.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Wave: ${String.format("%.1f", snapshot.marine.waveHeightMeters)}m",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NomadKitPalette.SecondaryOceanDark,
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
                            tint = if (place.alertEnabled) NomadKitPalette.PrimarySand else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
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
                                tint = NomadKitPalette.PrimarySand,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = NomadKitPalette.Error,
                                modifier = Modifier.size(18.dp)
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
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Thailand Place", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NomadKitPalette.PrimarySandDeep
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Search",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NomadKitPalette.PrimarySandDeep else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Coordinates",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NomadKitPalette.PrimarySandDeep else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        label = { Text("Search city, island or landmark") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NomadKitPalette.PrimarySand) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NomadKitPalette.PrimarySand, modifier = Modifier.size(24.dp))
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
                        label = { Text("Custom Group (e.g. Island / Coworking)") },
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NomadKitPalette.PrimarySand,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Location", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Cancel")
            }
        }
    )
}
