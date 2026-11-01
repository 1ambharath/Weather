package com.example.thailandweather.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.ui.components.getWeatherIcon
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
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
                        text = "Thailand Radar & Weather Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Overlays Selector Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MapOverlayType.values()) { type ->
                    val isSelected = uiState.selectedOverlay == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectOverlay(type) },
                        shape = RoundedCornerShape(percent = 50),
                        label = {
                            Text(
                                type.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DribbblePalette.Primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NomadKitPalette.Outline,
                            selectedBorderColor = DribbblePalette.Primary
                        ),
                        leadingIcon = {
                            val icon = when (type) {
                                MapOverlayType.RADAR -> Icons.Default.Radar
                                MapOverlayType.PRECIPITATION -> Icons.Default.WaterDrop
                                MapOverlayType.TEMPERATURE -> Icons.Default.Thermostat
                                MapOverlayType.WIND -> Icons.Default.Air
                                MapOverlayType.SATELLITE -> Icons.Default.SatelliteAlt
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            // Radar Station Selector
            if (uiState.selectedOverlay == MapOverlayType.RADAR) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.radarStations) { station ->
                        val isSelected = uiState.selectedStation.id == station.id
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isSelected) DribbblePalette.Primary else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) DribbblePalette.Primary else NomadKitPalette.Outline
                            ),
                            modifier = Modifier.clickable { viewModel.selectStation(station) }
                        ) {
                            Text(
                                text = station.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Map Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, NomadKitPalette.OutlineVariant, RoundedCornerShape(20.dp))
            ) {
                ThailandMapCanvas(
                    places = uiState.places,
                    inspectedPlace = uiState.inspectedPlace,
                    overlayType = uiState.selectedOverlay,
                    activeStation = uiState.selectedStation,
                    loopFrame = uiState.loopFrameIndex,
                    onPlaceTapped = { viewModel.inspectPlace(it) }
                )

                // Radar Legend
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.OutlineVariant),
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Reflectivity (dBZ)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.width(96.dp).height(8.dp).clip(RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF38BDF8)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF22C55E)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEAB308)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF97316)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEF4444)))
                        }
                        Row(
                            modifier = Modifier.width(96.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("10", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("35", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("60+", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Radar Loop Playback Controls
            RadarLoopControls(
                isPlaying = uiState.isPlayingLoop,
                frameIndex = uiState.loopFrameIndex,
                onTogglePlay = { viewModel.toggleLoopPlayback() },
                onFrameChanged = { viewModel.setLoopFrame(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Inspected Location Weather Card
            AnimatedVisibility(visible = uiState.inspectedPlace != null) {
                uiState.inspectedPlace?.let { place ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.PrimarySand)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${place.name}, ${place.province}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "GPS: ${String.format("%.2f", place.latitude)}°N, ${String.format("%.2f", place.longitude)}°E",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NomadKitPalette.SecondaryOcean
                                )
                                uiState.inspectedWeather?.let { snap ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${snap.weatherDescription} • Feels like ${Math.round(snap.apparentTemperatureCelsius)}°C",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NomadKitPalette.PrimarySandDeep
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                uiState.inspectedWeather?.let { snap ->
                                    Text(
                                        text = "${Math.round(snap.temperatureCelsius)}°C",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { viewModel.clearInspection() }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThailandMapCanvas(
    places: List<Place>,
    inspectedPlace: Place?,
    overlayType: MapOverlayType,
    activeStation: com.example.thailandweather.data.remote.TmdRadarStation,
    loopFrame: Int,
    onPlaceTapped: (Place) -> Unit
) {
    val minLat = 5.5
    val maxLat = 20.5
    val minLon = 97.0
    val maxLon = 106.0

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(places) {
                detectTapGestures { tapOffset ->
                    val width = size.width
                    val height = size.height

                    val tapped = places.minByOrNull { p ->
                        val x = ((p.longitude - minLon) / (maxLon - minLon) * width).toFloat()
                        val y = ((maxLat - p.latitude) / (maxLat - minLat) * height).toFloat()
                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        dx * dx + dy * dy
                    }
                    if (tapped != null) {
                        onPlaceTapped(tapped)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        fun toX(lon: Double): Float = ((lon - minLon) / (maxLon - minLon) * w).toFloat()
        fun toY(lat: Double): Float = ((maxLat - lat) / (maxLat - minLat) * h).toFloat()

        // 1. Draw generalized boundary shape of Thailand in warm earth tones
        val path = Path().apply {
            moveTo(toX(99.0), toY(20.0)) // Chiang Rai / North
            lineTo(toX(101.0), toY(19.5)) // Nan
            lineTo(toX(102.5), toY(18.0)) // Nong Khai
            lineTo(toX(105.5), toY(16.0)) // Ubon Ratchathani / Mekong
            lineTo(toX(103.0), toY(14.3)) // Surin / Cambodia border
            lineTo(toX(102.8), toY(12.0)) // Trat
            lineTo(toX(100.8), toY(12.9)) // Chonburi / Pattaya
            lineTo(toX(100.5), toY(13.6)) // Bangkok
            lineTo(toX(99.9), toY(12.5))  // Hua Hin
            lineTo(toX(99.3), toY(9.1))   // Surat Thani
            lineTo(toX(101.0), toY(6.5))  // Narathiwat (Deep South)
            lineTo(toX(100.1), toY(6.5))  // Satun
            lineTo(toX(98.3), toY(8.0))   // Phuket / Andaman
            lineTo(toX(98.5), toY(10.0))  // Ranong
            lineTo(toX(99.2), toY(11.5))  // Prachuap
            lineTo(toX(98.5), toY(14.5))  // Kanchanaburi
            lineTo(toX(97.5), toY(18.2))  // Mae Hong Son
            close()
        }

        // Draw territory fill & outline in NomadKit warm sand/stone
        drawPath(
            path = path,
            color = Color(0xFFD4A373).copy(alpha = 0.12f)
        )
        drawPath(
            path = path,
            color = Color(0xFFB08968).copy(alpha = 0.4f),
            style = Stroke(width = 2.dp.toPx())
        )

        // 2. Draw Radar / Precipitation Reflectivity Overlays in Ocean accents
        if (overlayType == MapOverlayType.RADAR || overlayType == MapOverlayType.PRECIPITATION) {
            val stationX = toX(activeStation.longitude)
            val stationY = toY(activeStation.latitude)

            // Radar range sweep circle with Ocean tint
            drawCircle(
                color = Color(0xFF0891B2).copy(alpha = 0.12f),
                radius = 80.dp.toPx(),
                center = Offset(stationX, stationY)
            )
            drawCircle(
                color = Color(0xFF0891B2).copy(alpha = 0.35f),
                radius = 80.dp.toPx(),
                center = Offset(stationX, stationY),
                style = Stroke(width = 1.dp.toPx())
            )

            // Simulated precipitation blobs around frame
            val frameOffset = loopFrame * 4.0
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFDC2626).copy(alpha = 0.65f),
                        Color(0xFFCA8A04).copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(stationX + frameOffset.toFloat(), stationY - frameOffset.toFloat()),
                    radius = 35.dp.toPx()
                ),
                radius = 35.dp.toPx(),
                center = Offset(stationX + frameOffset.toFloat(), stationY - frameOffset.toFloat())
            )
        }

        // 3. Draw Saved Place Pins in NomadKit Primary Sand
        places.forEach { place ->
            val px = toX(place.longitude)
            val py = toY(place.latitude)
            val isInspected = inspectedPlace?.id == place.id

            // Pin shadow / halo in warm sand
            drawCircle(
                color = if (isInspected) Color(0xFFD4A373).copy(alpha = 0.5f) else Color(0xFF0891B2).copy(alpha = 0.25f),
                radius = if (isInspected) 16.dp.toPx() else 10.dp.toPx(),
                center = Offset(px, py)
            )

            // Main pin dot
            drawCircle(
                color = if (isInspected) Color(0xFFB08968) else Color(0xFFD4A373),
                radius = if (isInspected) 7.dp.toPx() else 5.dp.toPx(),
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White,
                radius = if (isInspected) 3.5.dp.toPx() else 2.5.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
fun RadarLoopControls(
    isPlaying: Boolean,
    frameIndex: Int,
    onTogglePlay: () -> Unit,
    onFrameChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("-60m", "-45m", "-30m", "-15m", "LIVE")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(38.dp)
                    .background(NomadKitPalette.PrimarySand, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play loop",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = frameIndex.toFloat(),
                    onValueChange = { onFrameChanged(it.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = NomadKitPalette.PrimarySand,
                        activeTrackColor = NomadKitPalette.PrimarySand,
                        inactiveTrackColor = NomadKitPalette.PrimarySandLight
                    ),
                    modifier = Modifier.height(28.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEachIndexed { idx, label ->
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (idx == frameIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == frameIndex) NomadKitPalette.PrimarySandDeep else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
