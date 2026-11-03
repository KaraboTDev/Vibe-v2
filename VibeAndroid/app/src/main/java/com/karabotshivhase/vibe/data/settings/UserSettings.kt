package com.karabotshivhase.vibe.data.settings

data class UserSettings(
    val language: String = "en",
    val defaultAreaName: String? = null,
    val notificationsEnabled: Boolean = true,
)
