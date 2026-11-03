package com.karabotshivhase.vibe.data.settings

interface SettingsSource {
    fun current(): UserSettings
    suspend fun update(change: SettingsChange): UpdateResult
    suspend fun refresh(): RefreshResult
    fun clear()
}
