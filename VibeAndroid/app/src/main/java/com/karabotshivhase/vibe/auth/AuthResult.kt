package com.karabotshivhase.vibe.auth

import com.karabotshivhase.vibe.network.AuthResponse

enum class FailureKind {
    InvalidInput,
    InvalidCredentials,
    EmailTaken,
    Network,
    Unknown
}

sealed interface AuthResult {
    data class Success(val response: AuthResponse) : AuthResult
    data class Failure(val kind: FailureKind, val serverMessage: String?) : AuthResult
}
