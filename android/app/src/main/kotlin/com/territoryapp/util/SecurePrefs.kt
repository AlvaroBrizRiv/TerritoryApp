package com.territoryapp.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePrefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(token: String, refreshToken: String) {
        prefs.edit()
            .putString("token", token)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun saveUser(email: String, rol: String) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_rol", rol)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getUserEmail(): String? = prefs.getString("user_email", null)
    fun getUserRol(): String? = prefs.getString("user_rol", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
