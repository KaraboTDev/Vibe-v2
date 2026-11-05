package com.karabotshivhase.vibe.ui.settings

import com.karabotshivhase.vibe.auth.SessionStore
import com.karabotshivhase.vibe.network.AuthResponse

class FakeSessionStore : SessionStore {
    var tokenValue: String? = "fake-token"
    var emailValue: String? = "test@example.com"
    var userIdValue: Int? = 42

    var clearCallCount = 0

    fun token(): String? = tokenValue
    fun email(): String? = emailValue
    fun userId(): Int? = userIdValue

    override fun save(response: AuthResponse) {
        tokenValue = response.token
        emailValue = response.email
        userIdValue = response.userId
    }

    override fun clear() {
        clearCallCount++
        tokenValue = null
        emailValue = null
        userIdValue = null
    }
}
