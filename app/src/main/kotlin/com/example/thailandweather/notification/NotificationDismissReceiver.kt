package com.example.thailandweather.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * BroadcastReceiver triggered when a user taps "Dismiss" on an alert notification
 * or when an alert notification is swiped away.
 */
class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.example.thailandweather.notification.ACTION_DISMISS"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
