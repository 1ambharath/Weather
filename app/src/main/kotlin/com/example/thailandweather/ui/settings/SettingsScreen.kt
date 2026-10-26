package com.example.thailandweather.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Attribution", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Units Section
            item {
                Text(
                    text = "Units & Language",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Temperature Unit")
                            Row {
                                FilterChip(
                                    selected = prefs.tempUnitCelsius,
                                    onClick = { viewModel.toggleTempUnit(true) },
                                    label = { Text("Celsius (°C)") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = !prefs.tempUnitCelsius,
                                    onClick = { viewModel.toggleTempUnit(false) },
                                    label = { Text("Fahrenheit (°F)") }
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wind Speed Unit")
                            Row {
                                FilterChip(
                                    selected = prefs.windUnitKmh,
                                    onClick = { viewModel.toggleWindUnit(true) },
                                    label = { Text("km/h") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = !prefs.windUnitKmh,
                                    onClick = { viewModel.toggleWindUnit(false) },
                                    label = { Text("mph") }
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Display Language")
                            Row {
                                FilterChip(
                                    selected = !prefs.languageThai,
                                    onClick = { viewModel.toggleLanguage(false) },
                                    label = { Text("English") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = prefs.languageThai,
                                    onClick = { viewModel.toggleLanguage(true) },
                                    label = { Text("ไทย") }
                                )
                            }
                        }
                    }
                }
            }

            // Proximity & Clustering
            item {
                Text(
                    text = "Proximity & Clustering",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Default Clustering Radius (Haversine calculation)")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 25, 50).forEach { radius ->
                                FilterChip(
                                    selected = prefs.proximityRadiusKm == radius,
                                    onClick = { viewModel.setProximityRadius(radius) },
                                    label = { Text("$radius km") }
                                )
                            }
                        }
                    }
                }
            }

            // Notifications
            item {
                Text(
                    text = "Notification Channels",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Master Notifications", fontWeight = FontWeight.SemiBold)
                                Text("Enable background sync and alerts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = prefs.notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications(it) }
                            )
                        }

                        if (prefs.notificationsEnabled) {
                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Official TMD Warnings", fontSize = 14.sp)
                                Switch(
                                    checked = prefs.notifyOfficialWarnings,
                                    onCheckedChange = { viewModel.toggleOfficialWarnings(it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Forecast Risks (Heat / Rain / Gales)", fontSize = 14.sp)
                                Switch(
                                    checked = prefs.notifyForecastRisks,
                                    onCheckedChange = { viewModel.toggleForecastRisks(it) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Air Quality (PM2.5) & Marine", fontSize = 14.sp)
                                Switch(
                                    checked = prefs.notifyAirQuality,
                                    onCheckedChange = { viewModel.toggleAirQuality(it) }
                                )
                            }
                        }
                    }
                }
            }

            // Data Sources & Licensing Attribution
            item {
                Text(
                    text = "Data Sources & Legal Attribution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Open-Meteo Weather APIs",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Global forecast models, hourly outlooks, hydrology, marine weather and air quality are provided under the Creative Commons Attribution 4.0 International License (CC BY 4.0).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/en/terms"))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Open-Meteo Terms & Attribution License")
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "Thai Meteorological Department (TMD)",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Official weather warnings, CAP advisories, satellite cloud products, and Doppler radar imagery are sourced from the Thai Meteorological Department (tmd.go.th).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = "Pollution Control Department (Air4Thai)",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Air quality categories, PM2.5 thresholds, and protective health recommendations adhere to the Pollution Control Department (PCD) Thailand national standard.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // App Version Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thailand Weather Intelligence v1.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Native Android Jetpack Compose • Room Offline First",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
