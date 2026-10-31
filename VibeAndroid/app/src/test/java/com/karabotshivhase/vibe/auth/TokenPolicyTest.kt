package com.karabotshivhase.vibe.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TokenPolicyTest {

    @Test
    fun isExpired_future_returns_false() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val future = "2030-01-01T00:00:00Z"
        assertFalse(TokenPolicy.isExpired(future, now))
    }

    @Test
    fun isExpired_past_returns_true() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val past = "2020-01-01T00:00:00Z"
        assertTrue(TokenPolicy.isExpired(past, now))
    }

    @Test
    fun isExpired_null_blank_or_garbage_returns_true() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        assertTrue(TokenPolicy.isExpired(null, now))
        assertTrue(TokenPolicy.isExpired("", now))
        assertTrue(TokenPolicy.isExpired("   ", now))
        assertTrue(TokenPolicy.isExpired("invalid-date-format", now))
    }

    @Test
    fun isExpired_parses_seven_fractional_digits() {
        val expiresAt = "2026-09-22T03:02:18.8974058Z"
        val nowBefore = Instant.parse("2026-09-22T03:02:18.0000000Z")
        val nowAfter = Instant.parse("2026-09-22T03:02:19.0000000Z")

        assertFalse(TokenPolicy.isExpired(expiresAt, nowBefore))
        assertTrue(TokenPolicy.isExpired(expiresAt, nowAfter))
    }
}
