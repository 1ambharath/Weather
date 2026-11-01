package com.example.thailandweather.notification

import android.content.Context

interface RateLimitStorage {
    fun loadTimestamps(): List<Long>
    fun saveTimestamps(timestamps: List<Long>)
}

class SharedPreferencesRateLimitStorage(context: Context) : RateLimitStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadTimestamps(): List<Long> {
        val raw = prefs.getString(KEY_ALERT_TIMESTAMPS, "") ?: ""
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    override fun saveTimestamps(timestamps: List<Long>) {
        prefs.edit()
            .putString(KEY_ALERT_TIMESTAMPS, timestamps.joinToString(","))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "weather_notifications_rate_limit"
        private const val KEY_ALERT_TIMESTAMPS = "recent_alert_notification_timestamps"
    }
}

/**
 * Enforces a strict rate limit of maximum 2 alert notifications per rolling 1-hour window.
 */
class AlertNotificationRateLimiter(
    private val storage: RateLimitStorage,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val MAX_PER_HOUR = 2
        const val ONE_HOUR_MILLIS = 3600_000L
    }

    @Synchronized
    fun canSend(): Boolean {
        val now = clock()
        val validTimestamps = storage.loadTimestamps().filter { now - it < ONE_HOUR_MILLIS }
        return validTimestamps.size < MAX_PER_HOUR
    }

    @Synchronized
    fun recordSent() {
        val now = clock()
        val validTimestamps = storage.loadTimestamps()
            .filter { now - it < ONE_HOUR_MILLIS }
            .toMutableList()
        validTimestamps.add(now)
        storage.saveTimestamps(validTimestamps)
    }

    @Synchronized
    fun getCountInLastHour(): Int {
        val now = clock()
        return storage.loadTimestamps().count { now - it < ONE_HOUR_MILLIS }
    }

    @Synchronized
    fun reset() {
        storage.saveTimestamps(emptyList())
    }
}
