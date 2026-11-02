package com.karabotshivhase.vibe.data.favorites

interface SyncRunner {
    suspend fun sync(): SyncOutcome
}
