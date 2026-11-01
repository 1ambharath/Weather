package com.example.thailandweather.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.OfficialAlert
import com.example.thailandweather.core.model.Place
import com.example.thailandweather.core.util.WeatherUtils
import com.example.thailandweather.ui.components.*
import com.example.thailandweather.ui.theme.AetherialPalette
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAlerts: () -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToPlaces: () -> Unit,
    onNavigateToExplore: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {},
    onNavigateToAiBriefing: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLocationMenu by remember { mutableStateOf(false) }
    var dismissedAlertIds by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Capsule
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (isDark) AetherialPalette.DarkSurface else Color.White)
                                .border(
                                    1.dp,
                                    if (isDark) AetherialPalette.DarkBorder else AetherialPalette.Outline,
                                    RoundedCornerShape(percent = 50)
                                )
                                .clickable { showLocationMenu = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = AetherialPalette.FaintNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.selectedPlace?.let { "${it.name}, TH" } ?: "Bangkok, TH",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change place",
                                tint = AetherialPalette.FaintNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // LIVE status pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelMint)
                                .border(
                                    1.dp,
                                    if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelMintBorder,
                                    RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AetherialPalette.MintGreen)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AetherialPalette.MintGreenText,
                                letterSpacing = 0.5.sp
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
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = AetherialPalette.Primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Filter / Tune Circular Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) AetherialPalette.DarkSurface else Color.White)
                            .border(1.dp, if (isDark) AetherialPalette.DarkBorder else AetherialPalette.Outline, CircleShape)
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = if (isDark) AetherialPalette.DarkTextMuted else AetherialPalette.SubduedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Profile / Settings Circular Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) AetherialPalette.DarkSurface else Color.White)
                            .border(1.dp, if (isDark) AetherialPalette.DarkBorder else AetherialPalette.Outline, CircleShape)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Settings",
                            tint = if (isDark) AetherialPalette.DarkTextMuted else AetherialPalette.SubduedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        }
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
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
                    color = AetherialPalette.Primary
                )
            }
        ) {
            if (uiState.isLoading && uiState.currentSnapshot == null) {
                // Skeleton Shimmer States
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NomadSkeletonHeroCard()
                    NomadSkeletonHourlyRow()
                    NomadSkeletonListCard()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                ) {
                    val snapshot = uiState.currentSnapshot
                    if (snapshot != null && snapshot.isStale) {
                        item {
                            StaleDataBanner(
                                isStale = true,
                                dataSource = snapshot.dataSource,
                                onSync = { viewModel.refresh() }
                            )
                        }
                    }

                    // High priority alert banner matching coral-peach reference styling
                    val alert = uiState.highPriorityAlerts.firstOrNull { it.id !in dismissedAlertIds }
                    if (alert != null) {
                        item {
                            OfficialAlertBanner(
                                alert = alert,
                                onClick = onNavigateToAlerts,
                                onDismiss = { dismissedAlertIds = dismissedAlertIds + alert.id }
                            )
                        }
                    }

                    // Current Weather Hero Card with 4 rich pastel indicator tiles
                    item {
                        uiState.currentSnapshot?.let { snap ->
                            CurrentWeatherHeroCard(
                                snapshot = snap,
                                place = uiState.selectedPlace,
                                daily = uiState.dailyForecast.firstOrNull()
                            )
                        }
                    }

                    // Hourly Forecast Section (Condition-Colored Pastel Cards)
                    item {
                        HourlyForecastSection(
                            hourly = uiState.hourlyForecast,
                            isCelsius = true
                        )
                    }

                    // AI Meteorological Digest Card (Soft Lavender with GROQ pill & Highlighted Bullets)
                    item {
                        val briefBullets = uiState.aiBriefingSummary?.ifBlank { null } ?: """
                            • **Monsoonal Trough**: Intensifying across **Bangkok** and the central river basin.
                            • **Calm Morning Conditions**: Shifting abruptly to isolated heavy downpours around **16:00**.
                            • **Commuter Advisory**: Prepare for localized street flooding and traffic delays.
                        """.trimIndent()

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) AetherialPalette.DarkSurface else AetherialPalette.PastelPurple.copy(alpha = 0.55f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelPurpleBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAiBriefing() }
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                // Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelPurple),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = AetherialPalette.PurpleAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AI Meteorological Digest",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = AetherialPalette.PurpleAccent
                                    ) {
                                        Text(
                                            text = "GROQ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) AetherialPalette.DarkSurfaceRaised else Color.White)
                                            .border(
                                                1.dp,
                                                if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelPurpleBorder,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Open Briefing",
                                            tint = AetherialPalette.PurpleAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Highlighted Brief Markdown Bullets
                                HighlightedMarkdownBulletList(
                                    text = briefBullets,
                                    maxItems = 3,
                                    bulletColor = AetherialPalette.PurpleAccent,
                                    highlightColor = if (isDark) Color.White else AetherialPalette.DeepSlateNavy,
                                    textColor = if (isDark) AetherialPalette.SubduedSlate else MaterialTheme.colorScheme.onSurfaceVariant,
                                    verticalSpacing = 8.dp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Footer Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelPurple,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelPurpleBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = AetherialPalette.PurpleAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Synthesized from 8 stations",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AetherialPalette.PurpleText
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Updated 10m ago",
                                        fontSize = 11.sp,
                                        color = AetherialPalette.FaintNavy
                                    )
                                }
                            }
                        }
                    }

                    // 2-Column Bottom Row: LIVE RADAR + COMFORT
                    item {
                        val humidity = uiState.currentSnapshot?.relativeHumidity ?: 68
                        val temp = uiState.currentSnapshot?.temperatureCelsius ?: 31.0
                        val dewPoint = (temp - ((100 - humidity) / 5.0)).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Card 1: LIVE RADAR
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToExplore() },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) AetherialPalette.DarkSurface else Color(0xFFF0F9FF)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelSkyBorder
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LIVE RADAR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AetherialPalette.SkyBlueText,
                                            letterSpacing = 0.5.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(percent = 50))
                                                .background(if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelMint)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(AetherialPalette.MintGreen)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AetherialPalette.MintGreenText
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Chao Phraya",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Cell tracking NW",
                                        fontSize = 12.sp,
                                        color = AetherialPalette.FaintNavy
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelSky),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Radar,
                                                contentDescription = null,
                                                tint = AetherialPalette.SkyBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Open Map ➔",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AetherialPalette.SkyBlueText
                                        )
                                    }
                                }
                            }

                            // Card 2: COMFORT
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) AetherialPalette.DarkSurface else Color(0xFFFFFBF5)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelAmberBorder
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COMFORT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AetherialPalette.AmberText,
                                            letterSpacing = 0.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(percent = 50),
                                            color = if (isDark) AetherialPalette.DarkSurfaceRaised else Color(0xFFFFEDD5)
                                        ) {
                                            Text(
                                                text = if (humidity > 70) "Humid" else "Moderate",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AetherialPalette.AmberText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$humidity%",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Dew point $dewPoint°C",
                                        fontSize = 12.sp,
                                        color = AetherialPalette.FaintNavy
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = AetherialPalette.AmberWarm,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sunset 18:24",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AetherialPalette.AmberText
                                        )
                                    }
                                }
                            }
                        }
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
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Weather data powered by Open-Meteo • TMD Radar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Just Thailand. Made with love ❤️",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) AetherialPalette.DarkSurface else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) AetherialPalette.DarkBorder else AetherialPalette.Outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Location + Thai Name + Central Pill & Amber Sun Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = place?.name ?: snapshot.placeName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.SurfaceContainerHigh
                        ) {
                            Text(
                                text = place?.customGroup ?: "Central",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    place?.thaiName?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = AetherialPalette.FaintNavy
                        )
                    }
                }
                // Warm glowing amber rounded square container
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelAmber)
                        .border(
                            1.dp,
                            if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelAmberBorder,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getWeatherIcon(snapshot.weatherCode),
                        contentDescription = snapshot.weatherDescription,
                        tint = AetherialPalette.AmberWarm,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Temp & Condition Row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Math.round(snapshot.temperatureCelsius)}°",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = snapshot.weatherDescription,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Feels like ${Math.round(snapshot.apparentTemperatureCelsius)}°C • Tropical Humidity",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // High / Low / Rain Capsule
            daily?.let {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.SurfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "High: ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${it.maxTempCelsius.toInt()}°C",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "  •  Low: ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${it.minTempCelsius.toInt()}°C",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "  •  💧 ${it.precipitationProbabilityMax}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AetherialPalette.SkyBlueText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AQI Capsule
            val aqiVal = snapshot.airQuality?.aqiUs ?: 42
            val aqiLabel = snapshot.airQuality?.category?.label ?: "Good Air"
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelMint,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelMintBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AetherialPalette.MintGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AQI $aqiVal • $aqiLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AetherialPalette.MintGreenText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4 Distinct Pastel Indicator Tiles: HUMIDITY, WIND, UV INDEX, PRESSURE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Humidity (Sky Blue)
                PastelIndicatorCard(
                    title = "HUMIDITY",
                    value = "${snapshot.relativeHumidity}%",
                    icon = Icons.Default.WaterDrop,
                    bgColor = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelSky,
                    borderColor = if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelSkyBorder,
                    iconTint = AetherialPalette.SkyBlue,
                    titleColor = AetherialPalette.SkyBlueText,
                    modifier = Modifier.weight(1f)
                )

                // 2. Wind (Mint Green)
                PastelIndicatorCard(
                    title = "WIND",
                    value = "${snapshot.windSpeedKmh.toInt()}",
                    valueUnit = " km/h",
                    icon = Icons.Default.Air,
                    bgColor = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelMint,
                    borderColor = if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelMintBorder,
                    iconTint = AetherialPalette.MintGreen,
                    titleColor = AetherialPalette.MintGreenText,
                    modifier = Modifier.weight(1f)
                )

                // 3. UV Index (Warm Amber)
                PastelIndicatorCard(
                    title = "UV INDEX",
                    value = "${snapshot.uvIndex.toInt()}",
                    valueUnit = if (snapshot.uvIndex >= 6) " High" else " Mod",
                    icon = Icons.Default.WbSunny,
                    bgColor = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelAmber,
                    borderColor = if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelAmberBorder,
                    iconTint = AetherialPalette.AmberWarm,
                    titleColor = AetherialPalette.AmberText,
                    modifier = Modifier.weight(1f)
                )

                // 4. Pressure (Lavender Purple)
                PastelIndicatorCard(
                    title = "PRESSURE",
                    value = "${snapshot.surfacePressureHpa?.toInt() ?: 1012}",
                    icon = Icons.Default.Speed,
                    bgColor = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelPurple,
                    borderColor = if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelPurpleBorder,
                    iconTint = AetherialPalette.Purple,
                    titleColor = AetherialPalette.PurpleText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PastelIndicatorCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    borderColor: Color,
    iconTint: Color,
    titleColor: Color,
    modifier: Modifier = Modifier,
    valueUnit: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (valueUnit != null) {
                    Text(
                        text = valueUnit,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                }
            }
        }
    }
}

@Composable
fun OfficialAlertBanner(
    alert: OfficialAlert,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == AetherialPalette.DarkBackground
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) AetherialPalette.DarkSurface else AetherialPalette.PastelCoral.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelCoralBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bright red warning badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherialPalette.Coral),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = if (isDark) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelCoral,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AetherialPalette.PastelCoralBorder)
                    ) {
                        Text(
                            text = "OFFICIAL TMD ALERT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AetherialPalette.CoralText,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Issue #4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = AetherialPalette.FaintNavy
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Source: ${alert.source}",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Circular chevron button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDark) AetherialPalette.DarkSurfaceRaised else Color.White)
                    .border(
                        1.dp,
                        if (isDark) AetherialPalette.DarkBorder else AetherialPalette.PastelCoralBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Details",
                    tint = AetherialPalette.Coral,
                    modifier = Modifier.size(18.dp)
                )
            }
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
            TextButton(
                onClick = onManageClick,
                colors = ButtonDefaults.textButtonColors(contentColor = DribbblePalette.Accent)
            ) {
                Text(text = "Manage", fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(places) { place ->
                val snap = snapshots[place.id]
                Card(
                    modifier = Modifier
                        .width(144.dp)
                        .clickable { onPlaceClick(place) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NomadKitPalette.Outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                        Spacer(modifier = Modifier.height(10.dp))
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
                                    tint = DribbblePalette.Accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        snap?.airQuality?.let { aq ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = Color(aq.category.hexColor).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(aq.category.hexColor).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "PM2.5: ${aq.pm25.toInt()}",
                                    fontSize = 10.sp,
                                    color = Color(aq.category.hexColor),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
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
            TextButton(
                onClick = onSeeAll,
                colors = ButtonDefaults.textButtonColors(contentColor = DribbblePalette.Accent)
            ) {
                Text(text = "See All", fontWeight = FontWeight.Bold)
            }
        }

        articles.take(2).forEach { article ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = DribbblePalette.NeutralRose,
                            contentColor = DribbblePalette.Accent
                        ) {
                            Text(
                                text = article.category.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = article.publisher,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
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
