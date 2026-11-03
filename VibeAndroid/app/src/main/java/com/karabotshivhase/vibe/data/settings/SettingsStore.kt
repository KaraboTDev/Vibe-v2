package com.karabotshivhase.vibe.data.settings

import android.content.Context
import androidx.core.content.edit

interface SettingsStore {
    fun read(): UserSettings
    fun write(s: UserSettings)
    fun isDirty(): Boolean
    fun setDirty(value: Boolean)
    fun clear()
}

class SharedPreferencesSettingsStore(
    context: Context,
) : SettingsStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): UserSettings {
        val language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        val defaultArea = if (prefs.contains(KEY_DEFAULT_AREA)) {
            prefs.getString(KEY_DEFAULT_AREA, null)
        } else {
            null
        }
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        return UserSettings(
            language = LanguageCodes.normalize(language),
            defaultAreaName = defaultArea,
            notificationsEnabled = notificationsEnabled,
        )
    }

    override fun write(s: UserSettings) {
        prefs.edit {
            putString(KEY_LANGUAGE, LanguageCodes.normalize(s.language))
            if (s.defaultAreaName != null) {
                putString(KEY_DEFAULT_AREA, s.defaultAreaName)
            } else {
                remove(KEY_DEFAULT_AREA)
            }
            putBoolean(KEY_NOTIFICATIONS, s.notificationsEnabled)
        }
    }

    override fun isDirty(): Boolean = prefs.getBoolean(KEY_DIRTY, false)

    override fun setDirty(value: Boolean) {
        prefs.edit { putBoolean(KEY_DIRTY, value) }
    }

    override fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "vibe_settings_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_DEFAULT_AREA = "default_area"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_DIRTY = "dirty"
    }
}
