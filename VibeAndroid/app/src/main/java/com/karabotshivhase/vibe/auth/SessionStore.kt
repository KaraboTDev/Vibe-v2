package com.karabotshivhase.vibe.auth

import com.karabotshivhase.vibe.network.AuthResponse

interface SessionStore {
    fun save(response: AuthResponse)
    fun clear()
}
