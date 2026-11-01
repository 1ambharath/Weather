package com.example.thailandweather.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Interface defining secure encryption-at-rest operations for sensitive credentials.
 */
interface SecureKeyStorage {
    suspend fun saveApiKey(key: String)
    suspend fun getApiKey(): String?
    suspend fun hasApiKey(): Boolean
    suspend fun removeApiKey()
    suspend fun getMaskedKeyDisplay(): String?
}

/**
 * Secure storage implementation leveraging Android KeyStore with hardware-backed AES-256-GCM encryption.
 * The raw API key is NEVER stored in plaintext in SharedPreferences, Room, DataStore, files, or logs.
 * All backup rules explicitly exclude the preference container backing this storage.
 */
class KeystoreEncryptedKeyStorage(
    private val context: Context,
    private val keyAlias: String = KEY_ALIAS_DEFAULT,
    private val prefsName: String = PREFS_FILE_NAME
) : SecureKeyStorage {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    override suspend fun saveApiKey(key: String): Unit = withContext(Dispatchers.IO) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            removeApiKey()
            return@withContext
        }

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv // 12-byte GCM initialization vector
        val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))

        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encodedCiphertext = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        // Generate a masked snippet for display without exposing the real key (e.g. "gsk_...ab12")
        val maskedDisplay = createMaskedRepresentation(trimmed)

        sharedPreferences.edit()
            .putString(KEY_ENCRYPTED_DATA, encodedCiphertext)
            .putString(KEY_IV, encodedIv)
            .putString(KEY_MASKED_PREVIEW, maskedDisplay)
            .apply()
    }

    override suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        try {
            val encodedCiphertext = sharedPreferences.getString(KEY_ENCRYPTED_DATA, null) ?: return@withContext null
            val encodedIv = sharedPreferences.getString(KEY_IV, null) ?: return@withContext null

            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)

            val secretKey = getSecretKey() ?: return@withContext null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Never log exception details containing payload or key material
            null
        }
    }

    override suspend fun hasApiKey(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.contains(KEY_ENCRYPTED_DATA) && sharedPreferences.contains(KEY_IV)
    }

    override suspend fun getMaskedKeyDisplay(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_MASKED_PREVIEW, null)
    }

    override suspend fun removeApiKey(): Unit = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
        try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (_: Exception) {
            // Ignore keystore cleanup failures
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return getSecretKey() ?: generateSecretKey()
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_DEFAULT = "thailand_weather_groq_key_v1"
        private const val PREFS_FILE_NAME = "secure_groq_storage"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val KEY_ENCRYPTED_DATA = "enc_key_data"
        private const val KEY_IV = "enc_key_iv"
        private const val KEY_MASKED_PREVIEW = "masked_preview"

        fun createMaskedRepresentation(key: String): String {
            if (key.length <= 8) return "••••••••"
            val prefix = if (key.startsWith("gsk_")) "gsk_" else key.take(3)
            val suffix = key.takeLast(4)
            return "$prefix••••••••$suffix"
        }
    }
}
