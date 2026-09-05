package com.example.duonote

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurityStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(PIN_CIPHERTEXT)

    fun isWidgetRevealed(): Boolean = preferences.getBoolean(WIDGET_REVEALED, true)

    fun setWidgetRevealed(revealed: Boolean) {
        preferences.edit().putBoolean(WIDGET_REVEALED, revealed).apply()
    }

    fun savePin(pin: String) {
        require(pin.matches(Regex("\\d{4}")))
        val encrypted = encrypt(pin.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(PIN_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{4}"))) return false
        val encoded = preferences.getString(PIN_CIPHERTEXT, null) ?: return false
        return try {
            val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
            decrypt(encrypted).toString(StandardCharsets.UTF_8) == pin
        } catch (_: Exception) {
            false
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
        KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
            if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
        }
    }

    companion object {
        private const val PREFERENCES = "security_prefs"
        private const val PIN_CIPHERTEXT = "pin_ciphertext"
        private const val WIDGET_REVEALED = "widget_revealed"
        private const val KEY_ALIAS = "duonote_security_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"

        private fun secretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val generator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE)
            generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            return generator.generateKey()
        }

        private fun encrypt(value: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            return cipher.iv + cipher.doFinal(value)
        }

        private fun decrypt(value: ByteArray): ByteArray {
            val iv = value.copyOfRange(0, 12)
            val payload = value.copyOfRange(12, value.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            return cipher.doFinal(payload)
        }
    }
}

object AppSecuritySession {
    var appUnlocked: Boolean = false
}
