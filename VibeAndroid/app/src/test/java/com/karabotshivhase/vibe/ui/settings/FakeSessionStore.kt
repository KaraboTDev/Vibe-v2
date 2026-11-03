package com.karabotshivhase.vibe.ui.settings

import com.karabotshivhase.vibe.auth.SessionStore
import com.karabotshivhase.vibe.network.AuthResponse

class FakeSessionStore : SessionStore {
    var clearCallCount = 0

    override fun save(response: AuthResponse) {
    }

    override fun clear() {
        clearCallCount++
    }
}
