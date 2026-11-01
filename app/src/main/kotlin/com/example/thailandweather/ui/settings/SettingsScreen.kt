package com.example.thailandweather.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.R
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = NomadKitPalette.PrimarySand,
        checkedBorderColor = NomadKitPalette.PrimarySand,
        uncheckedThumbColor = NomadKitPalette.Outline,
        uncheckedTrackColor = NomadKitPalette.SurfaceVariant
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Settings & Attribution", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NomadKitPalette.PrimarySand
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // Theme & Appearance Section
            item {
                Text(
                    text = "Theme & Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NomadKitPalette.PrimarySandDeep
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (prefs.darkMode) DribbblePalette.DarkSurfaceRaised else DribbblePalette.NeutralRose
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (prefs.darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (prefs.darkMode) Color(0xFFFFD54F) else DribbblePalette.Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (prefs.darkMode) "Dark Mode" else "Light Mode",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (prefs.darkMode) "Deep ink canvas (#0E0E11)" else "Crisp white canvas (#FFFFFF)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NomadKitPalette.OnSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            colors = switchColors
                        )
                    }
                }
            }

            // Units Section
            item {
                Text(
                    text = "Units & Language",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NomadKitPalette.PrimarySandDeep
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Temperature Unit", fontWeight = FontWeight.Medium)
                            Row {
                                FilterChip(
                                    selected = prefs.tempUnitCelsius,
                                    onClick = { viewModel.toggleTempUnit(true) },
                                    label = { Text("Celsius (°C)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.PrimarySandLight,
                                        selectedLabelColor = NomadKitPalette.PrimarySandDeep
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = prefs.tempUnitCelsius,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.PrimarySand
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = !prefs.tempUnitCelsius,
                                    onClick = { viewModel.toggleTempUnit(false) },
                                    label = { Text("Fahrenheit (°F)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.PrimarySandLight,
                                        selectedLabelColor = NomadKitPalette.PrimarySandDeep
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = !prefs.tempUnitCelsius,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.PrimarySand
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = NomadKitPalette.OutlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wind Speed Unit", fontWeight = FontWeight.Medium)
                            Row {
                                FilterChip(
                                    selected = prefs.windUnitKmh,
                                    onClick = { viewModel.toggleWindUnit(true) },
                                    label = { Text("km/h") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.PrimarySandLight,
                                        selectedLabelColor = NomadKitPalette.PrimarySandDeep
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = prefs.windUnitKmh,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.PrimarySand
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = !prefs.windUnitKmh,
                                    onClick = { viewModel.toggleWindUnit(false) },
                                    label = { Text("mph") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.PrimarySandLight,
                                        selectedLabelColor = NomadKitPalette.PrimarySandDeep
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = !prefs.windUnitKmh,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.PrimarySand
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = NomadKitPalette.OutlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Display Language", fontWeight = FontWeight.Medium)
                            Row {
                                FilterChip(
                                    selected = !prefs.languageThai,
                                    onClick = { viewModel.toggleLanguage(false) },
                                    label = { Text("English") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.PrimarySandLight,
                                        selectedLabelColor = NomadKitPalette.PrimarySandDeep
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = !prefs.languageThai,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.PrimarySand
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = prefs.languageThai,
                                    onClick = { viewModel.toggleLanguage(true) },
                                    label = { Text("ไทย") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.PrimarySandLight,
                                        selectedLabelColor = NomadKitPalette.PrimarySandDeep
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = prefs.languageThai,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.PrimarySand
                                    )
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
                    color = NomadKitPalette.PrimarySandDeep
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Default Clustering Radius (Haversine calculation)")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 25, 50).forEach { radius ->
                                FilterChip(
                                    selected = prefs.proximityRadiusKm == radius,
                                    onClick = { viewModel.setProximityRadius(radius) },
                                    label = { Text("$radius km") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NomadKitPalette.SecondaryOceanLight,
                                        selectedLabelColor = NomadKitPalette.SecondaryOceanDark
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = prefs.proximityRadiusKm == radius,
                                        borderColor = NomadKitPalette.OutlineVariant,
                                        selectedBorderColor = NomadKitPalette.SecondaryOcean
                                    )
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
                    color = NomadKitPalette.PrimarySandDeep
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Master Notifications", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Enable background sync and alerts",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications(it) },
                                colors = switchColors
                            )
                        }

                        if (prefs.notificationsEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = NomadKitPalette.OutlineVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Official TMD Warnings", fontSize = 14.sp)
                                Switch(
                                    checked = prefs.notifyOfficialWarnings,
                                    onCheckedChange = { viewModel.toggleOfficialWarnings(it) },
                                    colors = switchColors
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
                                    onCheckedChange = { viewModel.toggleForecastRisks(it) },
                                    colors = switchColors
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
                                    onCheckedChange = { viewModel.toggleAirQuality(it) },
                                    colors = switchColors
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
                    color = NomadKitPalette.PrimarySandDeep
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            colors = ButtonDefaults.textButtonColors(contentColor = NomadKitPalette.SecondaryOcean),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Open-Meteo Terms & Attribution License", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = NomadKitPalette.OutlineVariant
                        )

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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = NomadKitPalette.OutlineVariant
                        )

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

            // App Version & Design System Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_circle),
                        contentDescription = "Thailand Weather App Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Thailand Weather Intelligence v1.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Styled with Dribbble Design System • Offline-First",
                        fontSize = 11.sp,
                        color = NomadKitPalette.PrimarySand,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
