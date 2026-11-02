package com.karabotshivhase.vibe.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceFormatterTest {

    @Test
    fun format_999_is_999_m() {
        val result = DistanceFormatter.format(999, Locale.US)
        assertEquals("999 m", result)
    }

    @Test
    fun format_1000_is_1_0_km() {
        val result = DistanceFormatter.format(1000, Locale.US)
        assertEquals("1.0 km", result)
    }

    @Test
    fun format_1768_is_1_8_km() {
        val result = DistanceFormatter.format(1768, Locale.US)
        assertEquals("1.8 km", result)
    }

    @Test
    fun format_zero_is_0_m() {
        val result = DistanceFormatter.format(0, Locale.US)
        assertEquals("0 m", result)
    }
}
