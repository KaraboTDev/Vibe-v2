package com.karabotshivhase.vibe.data.settings

import com.karabotshivhase.vibe.data.favorites.RemoteResult

data class RemoteSettings(
    val language: String,
    val defaultArea: String?,
    val notificationsEnabled: Boolean,
)

interface SettingsRemote {
    suspend fun fetch(): RemoteResult<RemoteSettings>
    suspend fun update(settings: RemoteSettings): RemoteResult<RemoteSettings>
}
