package com.karabotshivhase.vibe.data.settings

sealed interface SettingsChange {
    data class Language(val code: String) : SettingsChange
    data class Area(val name: String) : SettingsChange
    data class Notifications(val enabled: Boolean) : SettingsChange
}
