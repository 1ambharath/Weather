package com.example.thailandweather.core.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SecureKeyStorageTest {

    private class FakeSecureKeyStorage : SecureKeyStorage {
        private var storedEncryptedData: ByteArray? = null
        private var storedMasked: String? = null

        override suspend fun saveApiKey(key: String) {
            val trimmed = key.trim()
            if (trimmed.isEmpty()) {
                removeApiKey()
                return
            }
            // Simulate encryption by reversing bytes
            storedEncryptedData = trimmed.toByteArray(Charsets.UTF_8).reversedArray()
            storedMasked = KeystoreEncryptedKeyStorage.createMaskedRepresentation(trimmed)
        }

        override suspend fun getApiKey(): String? {
            val data = storedEncryptedData ?: return null
            return String(data.reversedArray(), Charsets.UTF_8)
        }

        override suspend fun hasApiKey(): Boolean {
            return storedEncryptedData != null
        }

        override suspend fun removeApiKey() {
            storedEncryptedData = null
            storedMasked = null
        }

        override suspend fun getMaskedKeyDisplay(): String? {
            return storedMasked
        }
    }

    @Test
    fun testMaskedRepresentation() {
        val shortKey = "12345"
        assertEquals("••••••••", KeystoreEncryptedKeyStorage.createMaskedRepresentation(shortKey))

        val standardKey = "gsk_1234567890abcdef"
        val masked = KeystoreEncryptedKeyStorage.createMaskedRepresentation(standardKey)
        assertTrue("Should start with gsk_", masked.startsWith("gsk_"))
        assertTrue("Should end with last 4 chars", masked.endsWith("cdef"))
        assertTrue("Should contain bullets", masked.contains("••••••••"))
        assertFalse("Should not leak middle characters", masked.contains("1234567890"))
    }

    @Test
    fun testSaveAndRetrieveKey() = runTest {
        val storage = FakeSecureKeyStorage()
        assertFalse(storage.hasApiKey())
        assertNull(storage.getApiKey())

        val testKey = "gsk_test_api_key_secret_123456789"
        storage.saveApiKey(testKey)

        assertTrue(storage.hasApiKey())
        assertEquals(testKey, storage.getApiKey())
        assertNotNull(storage.getMaskedKeyDisplay())
        assertTrue(storage.getMaskedKeyDisplay()!!.startsWith("gsk_"))
        assertTrue(storage.getMaskedKeyDisplay()!!.endsWith("6789"))
    }

    @Test
    fun testRemoveKeyCompletelyPurgesData() = runTest {
        val storage = FakeSecureKeyStorage()
        storage.saveApiKey("gsk_temporary_key_99999")
        assertTrue(storage.hasApiKey())

        storage.removeApiKey()

        assertFalse(storage.hasApiKey())
        assertNull(storage.getApiKey())
        assertNull(storage.getMaskedKeyDisplay())
    }

    @Test
    fun testSavingBlankKeyRemovesIt() = runTest {
        val storage = FakeSecureKeyStorage()
        storage.saveApiKey("gsk_valid_key_11111")
        assertTrue(storage.hasApiKey())

        storage.saveApiKey("   ")
        assertFalse(storage.hasApiKey())
        assertNull(storage.getApiKey())
    }
}
