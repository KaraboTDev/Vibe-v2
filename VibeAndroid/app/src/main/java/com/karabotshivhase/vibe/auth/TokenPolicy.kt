package com.karabotshivhase.vibe.auth

import java.time.Instant

object TokenPolicy {
    fun isExpired(expiresAt: String?, now: Instant = Instant.now()): Boolean {
        if (expiresAt.isNullOrBlank()) return true
        return try {
            val expiry = Instant.parse(expiresAt)
            !expiry.isAfter(now)
        } catch (_: Exception) {
            true
        }
    }
}
