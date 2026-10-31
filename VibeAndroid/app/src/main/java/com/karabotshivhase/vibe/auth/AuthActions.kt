package com.karabotshivhase.vibe.auth

interface AuthActions {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String): AuthResult
}
