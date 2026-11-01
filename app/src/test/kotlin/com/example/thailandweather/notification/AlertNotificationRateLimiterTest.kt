package com.example.thailandweather.notification

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AlertNotificationRateLimiterTest {

    private class FakeRateLimitStorage : RateLimitStorage {
        private var list = listOf<Long>()
        override fun loadTimestamps(): List<Long> = list
        override fun saveTimestamps(timestamps: List<Long>) {
            list = timestamps
        }
    }

    private lateinit var storage: FakeRateLimitStorage
    private var currentTime = 1_000_000_000L

    private lateinit var rateLimiter: AlertNotificationRateLimiter

    @Before
    fun setUp() {
        storage = FakeRateLimitStorage()
        currentTime = 1_000_000_000L
        rateLimiter = AlertNotificationRateLimiter(
            storage = storage,
            clock = { currentTime }
        )
    }

    @Test
    fun testInitialStateAllowsNotification() {
        assertTrue("Initially should allow sending alert notification", rateLimiter.canSend())
        assertEquals(0, rateLimiter.getCountInLastHour())
    }

    @Test
    fun testFirstNotificationAllowedAndRecorded() {
        assertTrue(rateLimiter.canSend())
        rateLimiter.recordSent()
        assertEquals(1, rateLimiter.getCountInLastHour())
        assertTrue("Should still allow 2nd notification in the same hour", rateLimiter.canSend())
    }

    @Test
    fun testMaxTwoNotificationsPerHourLimitEnforced() {
        // Send 1st
        assertTrue(rateLimiter.canSend())
        rateLimiter.recordSent()

        // Send 2nd
        assertTrue(rateLimiter.canSend())
        rateLimiter.recordSent()
        assertEquals(2, rateLimiter.getCountInLastHour())

        // 3rd attempt within the same hour MUST be blocked
        assertFalse("3rd notification in the same hour must be blocked by rate limiter", rateLimiter.canSend())

        // Even 30 minutes later, still blocked
        currentTime += 30 * 60 * 1000L // 30 minutes
        assertFalse("Must remain blocked at 30 minutes", rateLimiter.canSend())
        assertEquals(2, rateLimiter.getCountInLastHour())
    }

    @Test
    fun testNotificationAllowedAfterOneHourWindowExpires() {
        // Send 1st at T=0
        rateLimiter.recordSent()

        // Send 2nd at T=10 minutes
        currentTime += 10 * 60 * 1000L
        rateLimiter.recordSent()
        assertFalse(rateLimiter.canSend())

        // Advance time past 1 hour from 1st notification (T = 61 minutes)
        currentTime = 1_000_000_000L + (61 * 60 * 1000L)
        // 1st notification expired, only 2nd (sent at T=10m) is within window (now at 51 minutes ago)
        assertEquals(1, rateLimiter.getCountInLastHour())
        assertTrue("Should allow notification after oldest entry expires", rateLimiter.canSend())

        // Send 3rd notification at T=61m
        rateLimiter.recordSent()
        assertEquals(2, rateLimiter.getCountInLastHour())
        assertFalse("Should be blocked again after reaching 2", rateLimiter.canSend())
    }

    @Test
    fun testResetClearsRateLimit() {
        rateLimiter.recordSent()
        rateLimiter.recordSent()
        assertFalse(rateLimiter.canSend())

        rateLimiter.reset()
        assertTrue(rateLimiter.canSend())
        assertEquals(0, rateLimiter.getCountInLastHour())
    }
}
