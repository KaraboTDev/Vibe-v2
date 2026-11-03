package com.karabotshivhase.vibe.data.settings

enum class UpdateResult {
    Synced,
    SavedLocally,
    SessionExpired,
    InvalidInput,
    NotLoggedIn,
}

sealed interface RefreshResult {
    data class Updated(val settings: UserSettings) : RefreshResult
    object Unchanged : RefreshResult
    object Offline : RefreshResult
    object SessionExpired : RefreshResult
    object Failed : RefreshResult
    object Skipped : RefreshResult
}
