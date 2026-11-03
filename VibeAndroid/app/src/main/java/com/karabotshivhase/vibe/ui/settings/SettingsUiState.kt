package com.karabotshivhase.vibe.ui.settings

import androidx.annotation.StringRes

data class SettingsUiState(
    val language: String = "en",
    val areaName: String = "Hatfield",
    val notificationsEnabled: Boolean = true,
    val email: String? = null,
    val isBusy: Boolean = false,
    @StringRes val messageRes: Int? = null,
)

sealed interface SettingsEvent {
    data class ApplyLanguage(val code: String) : SettingsEvent
    object SessionExpired : SettingsEvent
    object NavigateLogin : SettingsEvent
}
