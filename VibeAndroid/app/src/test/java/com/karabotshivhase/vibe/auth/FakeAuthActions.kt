package com.karabotshivhase.vibe.auth

import kotlinx.coroutines.CompletableDeferred

class FakeAuthActions : AuthActions {
    var loginResult: AuthResult? = null
    var registerResult: AuthResult? = null

    var loginCallCount = 0
    var registerCallCount = 0

    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var lastRegisterEmail: String? = null
    var lastRegisterPassword: String? = null

    var suspendGate: CompletableDeferred<Unit>? = null

    override suspend fun login(email: String, password: String): AuthResult {
        loginCallCount++
        lastLoginEmail = email
        lastLoginPassword = password
        suspendGate?.await()
        return loginResult ?: AuthResult.Failure(FailureKind.Unknown, null)
    }

    override suspend fun register(email: String, password: String): AuthResult {
        registerCallCount++
        lastRegisterEmail = email
        lastRegisterPassword = password
        suspendGate?.await()
        return registerResult ?: AuthResult.Failure(FailureKind.Unknown, null)
    }
}
