package com.karabotshivhase.vibe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoursFormatterTest {

    @Test
    fun null_returns_null() {
        assertNull(HoursFormatter.clean(null))
    }

    @Test
    fun blank_returns_null() {
        assertNull(HoursFormatter.clean("   "))
        assertNull(HoursFormatter.clean(""))
    }

    @Test
    fun removes_space_before_comma() {
        val cleaned = HoursFormatter.clean("Mo-Fr 07:00-15:30 , Sa")
        assertEquals("Mo-Fr 07:00-15:30, Sa", cleaned)
    }

    @Test
    fun collapses_double_spaces_and_trims() {
        val cleaned = HoursFormatter.clean("  09:00   -   17:00  ")
        assertEquals("09:00 - 17:00", cleaned)
    }
}
