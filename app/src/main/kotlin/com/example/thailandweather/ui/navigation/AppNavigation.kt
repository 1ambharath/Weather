package com.example.thailandweather.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thailandweather.R
import com.example.thailandweather.WeatherApplication
import com.example.thailandweather.core.model.UserPreferences
import com.example.thailandweather.ui.alerts.AlertsScreen
import com.example.thailandweather.ui.alerts.AlertsViewModel
import com.example.thailandweather.ui.briefing.AiBriefingScreen
import com.example.thailandweather.ui.briefing.AiBriefingViewModel
import com.example.thailandweather.ui.explore.ExploreScreen
import com.example.thailandweather.ui.explore.ExploreViewModel
import com.example.thailandweather.ui.home.HomeScreen
import com.example.thailandweather.ui.home.HomeViewModel
import com.example.thailandweather.ui.news.NewsScreen
import com.example.thailandweather.ui.news.NewsViewModel
import com.example.thailandweather.ui.places.PlacesScreen
import com.example.thailandweather.ui.places.PlacesViewModel
import com.example.thailandweather.ui.settings.AiSettingsScreen
import com.example.thailandweather.ui.settings.AiSettingsViewModel
import com.example.thailandweather.ui.settings.SettingsScreen
import com.example.thailandweather.ui.settings.SettingsViewModel
import com.example.thailandweather.ui.theme.AetherialPalette
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette
import kotlinx.coroutines.launch

enum class NavigationTab(val label: String, val thaiLabel: String, val icon: ImageVector) {
    HOME("Today", "วันนี้", Icons.Default.CloudQueue),
    EXPLORE("Radar", "เรดาร์", Icons.Default.Radar),
    PLACES("Places", "สถานที่", Icons.Default.BookmarkBorder),
    NEWS("Forecast", "พยากรณ์", Icons.Default.CalendarToday),
    ALERTS("Alerts", "เตือนภัย", Icons.Default.WarningAmber)
}

sealed interface ScreenDestination {
    data class Tab(val tab: NavigationTab) : ScreenDestination
    object Settings : ScreenDestination
    object AiSettings : ScreenDestination
    object AiBriefing : ScreenDestination
}

@Composable
fun AppNavigation(
    app: WeatherApplication,
    initialTab: NavigationTab = NavigationTab.HOME,
    modifier: Modifier = Modifier
) {
    val userPreferences by app.preferencesDataStore.preferencesFlow.collectAsState(initial = UserPreferences())
    val isDarkMode = userPreferences.darkMode

    // Navigation back stack supporting sequential back gestures to previous screen
    val backStack = remember {
        mutableStateListOf<ScreenDestination>(ScreenDestination.Tab(initialTab))
    }

    val currentDestination = backStack.lastOrNull() ?: ScreenDestination.Tab(initialTab)

    val currentTab = when (currentDestination) {
        is ScreenDestination.Tab -> currentDestination.tab
        else -> (backStack.findLast { it is ScreenDestination.Tab } as? ScreenDestination.Tab)?.tab ?: initialTab
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    fun navigateTo(destination: ScreenDestination) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    // Rule 1: Back gesture closes navigation drawer if opened
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    // Rule 2: Back gesture brings user to previous screen they were on.
    // If backStack has reached the first screen (backStack.size == 1), BackHandler is disabled,
    // allowing the system back dispatcher to cleanly exit the app.
    BackHandler(enabled = !drawerState.isOpen && backStack.size > 1) {
        navigateBack()
    }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            app.placesRepository,
            app.weatherRepository,
            app.alertsRepository,
            app.newsRepository,
            app.aiRepository
        )
    )

    val exploreViewModel: ExploreViewModel = viewModel(
        factory = ExploreViewModel.Factory(
            app.placesRepository,
            app.weatherRepository
        )
    )

    val placesViewModel: PlacesViewModel = viewModel(
        factory = PlacesViewModel.Factory(
            app.placesRepository,
            app.weatherRepository,
            app.weatherProvider
        )
    )

    val newsViewModel: NewsViewModel = viewModel(
        factory = NewsViewModel.Factory(app.newsRepository)
    )

    val alertsViewModel: AlertsViewModel = viewModel(
        factory = AlertsViewModel.Factory(app.alertsRepository)
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.preferencesDataStore)
    )

    val aiSettingsViewModel: AiSettingsViewModel = viewModel(
        factory = AiSettingsViewModel.Factory(app.aiRepository)
    )

    val aiBriefingViewModel: AiBriefingViewModel = viewModel(
        factory = AiBriefingViewModel.Factory(app.aiRepository, app.placesRepository)
    )

    when (currentDestination) {
        is ScreenDestination.AiSettings -> {
            AiSettingsScreen(
                viewModel = aiSettingsViewModel,
                onNavigateBack = { navigateBack() }
            )
        }
        is ScreenDestination.AiBriefing -> {
            AiBriefingScreen(
                viewModel = aiBriefingViewModel,
                onNavigateBack = { navigateBack() },
                onNavigateToAiSettings = { navigateTo(ScreenDestination.AiSettings) }
            )
        }
        is ScreenDestination.Settings -> {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navigateBack() }
            )
        }
        is ScreenDestination.Tab -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerTonalElevation = 2.dp,
                        modifier = Modifier.width(320.dp)
                    ) {
                        // Drawer Header with Dribbble branding
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDarkMode) DribbblePalette.DarkSurfaceRaised else DribbblePalette.NeutralRose
                                )
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo_circle),
                                    contentDescription = "Thailand Weather App Logo",
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Thailand Weather",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) DribbblePalette.DarkText else DribbblePalette.TextPrimary
                                    )
                                    Text(
                                        text = "Aetherial Pastels Edition",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Menu Item: Dark / Light Mode Switch in Side Bar Menu
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) DribbblePalette.DarkSurfaceRaised else DribbblePalette.SurfaceMuted,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isDarkMode) DribbblePalette.DarkBorder else DribbblePalette.Hairline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDarkMode) DribbblePalette.DarkSurface else DribbblePalette.NeutralRose
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = if (isDarkMode) "Dark Mode" else "Light Mode",
                                            tint = if (isDarkMode) Color(0xFFFFD54F) else DribbblePalette.Accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isDarkMode) "Dark ink canvas" else "Crisp white canvas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { enabled ->
                                        coroutineScope.launch {
                                            app.preferencesDataStore.setDarkMode(enabled)
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            tint = if (isDarkMode) DribbblePalette.Primary else Color.White
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = DribbblePalette.Accent,
                                        checkedBorderColor = DribbblePalette.Accent,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = if (isDarkMode) DribbblePalette.DarkBorder else Color(0xFFC4C4C8),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        // Menu Item: AI Settings
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = DribbblePalette.Accent
                                )
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "AI Settings",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Configure your AI provider",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navigateTo(ScreenDestination.AiSettings)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )

                        // Menu Item: AI Briefing
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = DribbblePalette.Accent
                                )
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "AI Briefing",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Consolidated weather intelligence",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navigateTo(ScreenDestination.AiBriefing)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )

                        // Menu Item: App Settings
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Column {
                                    Text(
                                        text = "App Settings",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Units, language & attribution",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navigateTo(ScreenDestination.Settings)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = NomadKitPalette.OutlineVariant
                        )

                        Text(
                            text = "Quick Navigation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )

                        NavigationTab.values().forEach { tab ->
                            NavigationDrawerItem(
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                selected = currentTab == tab,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    navigateTo(ScreenDestination.Tab(tab))
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = if (isDarkMode) DribbblePalette.DarkSurfaceRaised else DribbblePalette.NeutralRose,
                                    selectedIconColor = if (isDarkMode) DribbblePalette.Accent else DribbblePalette.Primary,
                                    selectedTextColor = if (isDarkMode) DribbblePalette.DarkText else DribbblePalette.Primary,
                                    unselectedIconColor = if (isDarkMode) DribbblePalette.DarkTextMuted else DribbblePalette.TextMuted,
                                    unselectedTextColor = if (isDarkMode) DribbblePalette.DarkTextMuted else DribbblePalette.TextMuted
                                )
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = if (isDarkMode) AetherialPalette.DarkSurface else Color.White,
                            tonalElevation = 0.dp,
                            modifier = Modifier.border(
                                BorderStroke(
                                    1.dp,
                                    if (isDarkMode) AetherialPalette.DarkBorder else AetherialPalette.Outline
                                )
                            )
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { navigateTo(ScreenDestination.Tab(tab)) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = if (isDarkMode) AetherialPalette.DarkAccent else AetherialPalette.IndigoAccent,
                                        selectedTextColor = if (isDarkMode) AetherialPalette.DarkAccent else AetherialPalette.IndigoAccent,
                                        indicatorColor = if (isDarkMode) AetherialPalette.DarkSurfaceRaised else AetherialPalette.PastelIndigo,
                                        unselectedIconColor = if (isDarkMode) AetherialPalette.DarkTextMuted else AetherialPalette.FaintNavy,
                                        unselectedTextColor = if (isDarkMode) AetherialPalette.DarkTextMuted else AetherialPalette.FaintNavy
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    when (currentTab) {
                        NavigationTab.HOME -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToAlerts = { navigateTo(ScreenDestination.Tab(NavigationTab.ALERTS)) },
                            onNavigateToNews = { navigateTo(ScreenDestination.Tab(NavigationTab.NEWS)) },
                            onNavigateToPlaces = { navigateTo(ScreenDestination.Tab(NavigationTab.PLACES)) },
                            onNavigateToExplore = { navigateTo(ScreenDestination.Tab(NavigationTab.EXPLORE)) },
                            onNavigateToSettings = { navigateTo(ScreenDestination.Settings) },
                            onNavigateToAiSettings = { navigateTo(ScreenDestination.AiSettings) },
                            onNavigateToAiBriefing = { navigateTo(ScreenDestination.AiBriefing) },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            modifier = contentModifier
                        )
                        NavigationTab.EXPLORE -> ExploreScreen(
                            viewModel = exploreViewModel,
                            modifier = contentModifier
                        )
                        NavigationTab.PLACES -> PlacesScreen(
                            viewModel = placesViewModel,
                            onPlaceSelected = { place ->
                                homeViewModel.selectPlace(place)
                                navigateTo(ScreenDestination.Tab(NavigationTab.HOME))
                            },
                            modifier = contentModifier
                        )
                        NavigationTab.NEWS -> NewsScreen(
                            viewModel = newsViewModel,
                            modifier = contentModifier
                        )
                        NavigationTab.ALERTS -> AlertsScreen(
                            viewModel = alertsViewModel,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
