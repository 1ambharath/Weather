package com.example.thailandweather.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thailandweather.WeatherApplication
import com.example.thailandweather.ui.alerts.AlertsScreen
import com.example.thailandweather.ui.alerts.AlertsViewModel
import com.example.thailandweather.ui.explore.ExploreScreen
import com.example.thailandweather.ui.explore.ExploreViewModel
import com.example.thailandweather.ui.home.HomeScreen
import com.example.thailandweather.ui.home.HomeViewModel
import com.example.thailandweather.ui.news.NewsScreen
import com.example.thailandweather.ui.news.NewsViewModel
import com.example.thailandweather.ui.places.PlacesScreen
import com.example.thailandweather.ui.places.PlacesViewModel
import com.example.thailandweather.ui.settings.SettingsScreen
import com.example.thailandweather.ui.settings.SettingsViewModel

enum class NavigationTab(val label: String, val thaiLabel: String, val icon: ImageVector) {
    HOME("Home", "หน้าหลัก", Icons.Default.Home),
    EXPLORE("Explore", "สำรวจ & เรดาร์", Icons.Default.Explore),
    PLACES("Places", "สถานที่", Icons.Default.LocationCity),
    NEWS("News", "ข่าวสาร", Icons.Default.Article),
    ALERTS("Alerts", "การแจ้งเตือน", Icons.Default.Warning)
}

@Composable
fun AppNavigation(
    app: WeatherApplication,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showSettings by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            app.placesRepository,
            app.weatherRepository,
            app.alertsRepository,
            app.newsRepository
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

    if (showSettings) {
        SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { showSettings = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (currentTab) {
                NavigationTab.HOME -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAlerts = { currentTab = NavigationTab.ALERTS },
                    onNavigateToNews = { currentTab = NavigationTab.NEWS },
                    onNavigateToPlaces = { currentTab = NavigationTab.PLACES },
                    modifier = Modifier.padding(innerPadding)
                )
                NavigationTab.EXPLORE -> ExploreScreen(
                    viewModel = exploreViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                NavigationTab.PLACES -> PlacesScreen(
                    viewModel = placesViewModel,
                    onPlaceSelected = { place ->
                        homeViewModel.selectPlace(place)
                        currentTab = NavigationTab.HOME
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                NavigationTab.NEWS -> NewsScreen(
                    viewModel = newsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                NavigationTab.ALERTS -> AlertsScreen(
                    viewModel = alertsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
