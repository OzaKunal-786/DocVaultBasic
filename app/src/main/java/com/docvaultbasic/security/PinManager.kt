package com.docvaultbasic.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class PinManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "docvault_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    fun isPinSet(): Boolean {
        return sharedPreferences.contains(KEY_PIN_HASH)
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        sharedPreferences.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null)
        return storedHash == hashPin(pin)
    }

    fun isOnboardingComplete(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete(complete: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun getDatabasePassphrase(): ByteArray {
        // In a real app, this should be more complex or derived from the PIN
        // and stored securely. For this implementation, we'll use a fixed derived key
        // if the PIN is set, or a default one.
        val pinHash = sharedPreferences.getString(KEY_PIN_HASH, "default_passphrase")
        return pinHash!!.toByteArray().sliceArray(0 until 32)
    }
}
