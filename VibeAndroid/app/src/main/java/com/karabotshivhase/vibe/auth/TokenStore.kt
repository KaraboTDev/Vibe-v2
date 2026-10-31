package com.karabotshivhase.vibe.auth

import android.content.Context
import androidx.core.content.edit
import com.karabotshivhase.vibe.network.AuthResponse

class TokenStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun save(response: AuthResponse) {
        prefs.edit {
            putString(KEY_TOKEN, response.token)
            putInt(KEY_USER_ID, response.userId)
            putString(KEY_EMAIL, response.email)
            putString(KEY_EXPIRES_AT, response.expiresAt)
        }
    }

    override fun clear() {
        prefs.edit { clear() }
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun email(): String? = prefs.getString(KEY_EMAIL, null)

    fun userId(): Int? = if (prefs.contains(KEY_USER_ID)) prefs.getInt(KEY_USER_ID, -1) else null

    fun expiresAt(): String? = prefs.getString(KEY_EXPIRES_AT, null)

    fun isLoggedIn(): Boolean {
        val t = token()
        return !t.isNullOrEmpty() && !TokenPolicy.isExpired(expiresAt())
    }

    companion object {
        private const val PREFS_NAME = "vibe_auth_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
