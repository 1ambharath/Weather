package com.example.thailandweather.ui.alerts

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.ForecastRisk
import com.example.thailandweather.core.model.OfficialAlert
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.ui.components.HazardBadge
import com.example.thailandweather.ui.components.NomadSkeletonListCard
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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
                        text = "Weather Warnings & Hazards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        val alerts = uiState.officialAlerts
                        val helper = com.example.thailandweather.WeatherApplication.instance.notificationHelper
                        val count = helper.getRecentAlertNotificationsCount()
                        val alertToSend = alerts.getOrNull(count % (if (alerts.isNotEmpty()) alerts.size else 1)) ?: alerts.firstOrNull()
                        if (alertToSend != null) {
                            val sent = helper.showOfficialWarning(alertToSend, bypassCooldown = true)
                            if (sent) {
                                val newCount = helper.getRecentAlertNotificationsCount()
                                Toast.makeText(context, "Alert notification sent ($newCount/2 used this hour)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Rate limit reached: max 2 notifications per hour (2/2 sent)", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No official alerts available ($count/2 sent this hour)", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert Notification",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshAlerts() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = DribbblePalette.Accent
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
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
                            "Official TMD (${uiState.officialAlerts.size})",
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
                            "Forecast Risks (${uiState.forecastRisks.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) NomadKitPalette.PrimarySandDeep else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.isRefreshing && uiState.officialAlerts.isEmpty() && uiState.forecastRisks.isEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    NomadSkeletonListCard()
                    NomadSkeletonListCard()
                    NomadSkeletonListCard()
                }
            } else if (selectedTab == 0) {
                // Official Warnings
                if (uiState.officialAlerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.OutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NomadKitPalette.TertiaryForest,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All Clear in Monitored Regions",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "No active official weather warnings issued by TMD Thailand.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.officialAlerts) { alert ->
                            OfficialAlertCard(
                                alert = alert,
                                onClick = { viewModel.selectOfficialAlert(alert) }
                            )
                        }
                    }
                }
            } else {
                // Forecast Risks
                if (uiState.forecastRisks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.OutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NomadKitPalette.TertiaryForest,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Severe Forecast Hazards",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Forecast thresholds indicate safe conditions for all saved destinations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.forecastRisks) { risk ->
                            ForecastRiskCard(
                                risk = risk,
                                onClick = { viewModel.selectForecastRisk(risk) }
                            )
                        }
                    }
                }
            }
        }
        }
    }

    // Official Alert Detail Sheet
    uiState.selectedOfficialAlert?.let { alert ->
        val alertColor = Color(alert.severity.hexColor)
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { viewModel.selectOfficialAlert(null) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                HazardBadge(severity = alert.severity, isOfficial = true)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                alert.titleThai?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Source: ${alert.source} • Issued: ${WeatherUtils.formatDate(alert.issuedAt)} ${WeatherUtils.formatTime(alert.issuedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Expires: ${WeatherUtils.formatDate(alert.expiresAt)} ${WeatherUtils.formatTime(alert.expiresAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = alertColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = alert.description, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)

                alert.instruction?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Safety Instructions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = alertColor)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Affected Areas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(alert.affectedProvinces) { prov ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NomadKitPalette.SurfaceVariant
                        ) {
                            Text(
                                text = prov,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                alert.sourceUrl?.let { url ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DribbblePalette.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Official Warning on TMD Portal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Forecast Risk Detail Sheet
    uiState.selectedForecastRisk?.let { risk ->
        val riskColor = Color(risk.severity.hexColor)
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { viewModel.selectForecastRisk(null) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                HazardBadge(severity = risk.severity, isOfficial = false)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = risk.headline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "App Forecast Estimation for ${risk.placeName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Triggered by: ${risk.triggerValue}",
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Hazard Assessment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = risk.detail, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Window: ${WeatherUtils.formatTime(risk.estimatedWindowStart)} – ${WeatherUtils.formatTime(risk.estimatedWindowEnd)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OfficialAlertCard(
    alert: OfficialAlert,
    onClick: () -> Unit
) {
    val alertColor = Color(alert.severity.hexColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HazardBadge(severity = alert.severity, isOfficial = true)
                Text(
                    text = "Expires ${WeatherUtils.formatTime(alert.expiresAt)}",
                    fontSize = 11.sp,
                    color = alertColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Affected: ${alert.affectedProvinces.take(3).joinToString(", ")}${if (alert.affectedProvinces.size > 3) "..." else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = DribbblePalette.Accent
                )
            }
        }
    }
}

@Composable
fun ForecastRiskCard(
    risk: ForecastRisk,
    onClick: () -> Unit
) {
    val riskColor = Color(risk.severity.hexColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HazardBadge(severity = risk.severity, isOfficial = false)
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = DribbblePalette.NeutralRose
                ) {
                    Text(
                        text = risk.placeName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DribbblePalette.Accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = risk.headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = risk.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = riskColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = risk.triggerValue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = riskColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = DribbblePalette.Accent
                )
            }
        }
    }
}
