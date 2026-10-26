package com.example.thailandweather.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.thailandweather.MainActivity
import com.example.thailandweather.R
import com.example.thailandweather.core.model.ForecastRisk
import com.example.thailandweather.core.model.OfficialAlert
import java.util.concurrent.ConcurrentHashMap

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val cooldownTracker = ConcurrentHashMap<String, Long>()
    private val COOLDOWN_MILLIS = 6 * 3600 * 1000L // 6 hours cooldown per alert ID

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val officialChannel = NotificationChannel(
                CHANNEL_OFFICIAL_WARNINGS,
                "Official TMD & Disaster Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority official weather warnings and safety declarations from Thai authorities"
                enableVibration(true)
            }

            val forecastRiskChannel = NotificationChannel(
                CHANNEL_FORECAST_RISKS,
                "Forecast Risks & Weather Hazards",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "App-computed meteorological risks including extreme heat, heavy downpours and gales"
            }

            val airMarineChannel = NotificationChannel(
                CHANNEL_AIR_AND_MARINE,
                "Air Quality & Marine Advisories",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "PM2.5 pollution alerts and coastal swell notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(officialChannel, forecastRiskChannel, airMarineChannel)
            )
        }
    }

    fun showOfficialWarning(alert: OfficialAlert) {
        val lastNotified = cooldownTracker[alert.id] ?: 0L
        if (System.currentTimeMillis() - lastNotified < COOLDOWN_MILLIS) {
            return // Skip notification due to cooldown
        }
        cooldownTracker[alert.id] = System.currentTimeMillis()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_OFFICIAL_WARNINGS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Official Warning: ${alert.title}")
            .setContentText(alert.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.description}\n\nAffected: ${alert.affectedProvinces.joinToString(", ")}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not granted
        }
    }

    fun showForecastRisk(risk: ForecastRisk) {
        val lastNotified = cooldownTracker[risk.id] ?: 0L
        if (System.currentTimeMillis() - lastNotified < COOLDOWN_MILLIS) {
            return
        }
        cooldownTracker[risk.id] = System.currentTimeMillis()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            risk.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (risk.riskType == com.example.thailandweather.core.model.RiskType.HIGH_PM25 ||
            risk.riskType == com.example.thailandweather.core.model.RiskType.ROUGH_SEAS
        ) CHANNEL_AIR_AND_MARINE else CHANNEL_FORECAST_RISKS

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Forecast Hazard: ${risk.headline}")
            .setContentText("${risk.placeName}: ${risk.detail}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${risk.detail}\nValue: ${risk.triggerValue}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(risk.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    companion object {
        const val CHANNEL_OFFICIAL_WARNINGS = "official_warnings"
        const val CHANNEL_FORECAST_RISKS = "forecast_risks"
        const val CHANNEL_AIR_AND_MARINE = "air_and_marine"
    }
}
