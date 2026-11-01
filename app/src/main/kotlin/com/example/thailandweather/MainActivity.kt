package com.example.thailandweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.thailandweather.core.model.UserPreferences
import com.example.thailandweather.ui.navigation.AppNavigation
import com.example.thailandweather.ui.splash.SplashScreen
import com.example.thailandweather.ui.theme.ThailandWeatherTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val app = WeatherApplication.instance
      val prefs by app.preferencesDataStore.preferencesFlow.collectAsState(initial = UserPreferences())
      val isDark = prefs.darkMode

      var showSplash by remember { mutableStateOf(true) }

      // Notification permission request for Android 13+ (API 33+)
      val notificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
      ) { /* Permission response handled */ }

      LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              if (ContextCompat.checkSelfPermission(
                      this@MainActivity,
                      Manifest.permission.POST_NOTIFICATIONS
                  ) != PackageManager.PERMISSION_GRANTED
              ) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
          }
      }

      ThailandWeatherTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Crossfade(
            targetState = showSplash,
            animationSpec = tween(durationMillis = 500),
            label = "SplashTransition"
          ) { isSplash ->
            if (isSplash) {
              SplashScreen(onSplashFinished = { showSplash = false })
            } else {
              val navigateToAlerts = intent?.getStringExtra("navigate_to") == "alerts"
              val initialTab = if (navigateToAlerts) com.example.thailandweather.ui.navigation.NavigationTab.ALERTS else com.example.thailandweather.ui.navigation.NavigationTab.HOME
              AppNavigation(app = app, initialTab = initialTab)
            }
          }
        }
      }
    }
  }
}
