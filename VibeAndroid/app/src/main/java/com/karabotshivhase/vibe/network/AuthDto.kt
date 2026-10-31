package com.karabotshivhase.vibe.network

data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: Int,
    val email: String,
    val expiresAt: String
)

data class ErrorBody(
    val message: String?
)
