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

/**
 * NotificationHelper manages environmental and disaster alert notifications.
 *
 * Implements strict requirements:
 * - Rate limited to a maximum of 2 notifications per hour across alerts.
 * - Fully dismissible: user can swipe away, tap to open & dismiss, or tap explicit "Dismiss" action.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val cooldownTracker = ConcurrentHashMap<String, Long>()
    private val COOLDOWN_MILLIS = 3 * 3600 * 1000L // 3 hours cooldown per identical alert ID

    private val rateLimiter = AlertNotificationRateLimiter(SharedPreferencesRateLimitStorage(context))

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

    /**
     * Checks if a new alert notification is permitted under the 2 notifications per hour limit.
     */
    fun canSendAlertNotification(): Boolean = rateLimiter.canSend()

    /**
     * Returns count of alert notifications sent in the rolling past 1 hour.
     */
    fun getRecentAlertNotificationsCount(): Int = rateLimiter.getCountInLastHour()

    /**
     * Clears recent rate limit timestamps (useful for testing or manual user reset).
     */
    fun resetRateLimitTracker() {
        rateLimiter.reset()
        cooldownTracker.clear()
    }

    /**
     * Shows an official TMD warning notification if rate limit (max 2/hour) and cooldown permit.
     * Returns true if notification was shown, false if rate limited or on cooldown.
     */
    fun showOfficialWarning(alert: OfficialAlert, bypassCooldown: Boolean = false): Boolean {
        val lastNotified = cooldownTracker[alert.id] ?: 0L
        if (!bypassCooldown && System.currentTimeMillis() - lastNotified < COOLDOWN_MILLIS) {
            return false // Skip notification due to identical alert cooldown
        }

        if (!canSendAlertNotification()) {
            return false // Enforce max 2 alert notifications per hour
        }

        val notificationId = alert.id.hashCode()

        // Content intent when tapping notification
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent
        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = NotificationDismissReceiver.ACTION_DISMISS
            putExtra(NotificationDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_OFFICIAL_WARNINGS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Official Warning: ${alert.title}")
            .setContentText(alert.description)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${alert.description}\n\nAffected: ${alert.affectedProvinces.joinToString(", ")}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false) // User can swipe to dismiss
            .setDeleteIntent(dismissPendingIntent) // Called when user swipes away
            .addAction(0, "Dismiss", dismissPendingIntent) // Explicit Dismiss action button

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            cooldownTracker[alert.id] = System.currentTimeMillis()
            rateLimiter.recordSent()
            true
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
            false
        }
    }

    /**
     * Shows a forecast hazard risk notification if rate limit (max 2/hour) and cooldown permit.
     * Returns true if notification was shown, false if rate limited or on cooldown.
     */
    fun showForecastRisk(risk: ForecastRisk, bypassCooldown: Boolean = false): Boolean {
        val lastNotified = cooldownTracker[risk.id] ?: 0L
        if (!bypassCooldown && System.currentTimeMillis() - lastNotified < COOLDOWN_MILLIS) {
            return false
        }

        if (!canSendAlertNotification()) {
            return false // Enforce max 2 alert notifications per hour
        }

        val notificationId = risk.id.hashCode()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = NotificationDismissReceiver.ACTION_DISMISS
            putExtra(NotificationDismissReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (risk.riskType == com.example.thailandweather.core.model.RiskType.HIGH_PM25 ||
            risk.riskType == com.example.thailandweather.core.model.RiskType.ROUGH_SEAS
        ) CHANNEL_AIR_AND_MARINE else CHANNEL_FORECAST_RISKS

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Forecast Hazard: ${risk.headline}")
            .setContentText("${risk.placeName}: ${risk.detail}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${risk.detail}\nValue: ${risk.triggerValue}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false) // User can swipe to dismiss
            .setDeleteIntent(dismissPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent) // Explicit Dismiss action button

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            cooldownTracker[risk.id] = System.currentTimeMillis()
            rateLimiter.recordSent()
            true
        } catch (e: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_OFFICIAL_WARNINGS = "official_warnings"
        const val CHANNEL_FORECAST_RISKS = "forecast_risks"
        const val CHANNEL_AIR_AND_MARINE = "air_and_marine"
    }
}
