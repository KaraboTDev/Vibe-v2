package com.karabotshivhase.vibe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaPresetsTest {

    @Test
    fun names_are_unique() {
        val names = AreaPresets.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun coordinates_are_valid_ranges() {
        for (area in AreaPresets.all) {
            assertTrue(area.latitude in -90.0..90.0)
            assertTrue(area.longitude in -180.0..180.0)
        }
    }

    @Test
    fun byName_is_case_insensitive_and_falls_back_to_default() {
        val found = AreaPresets.byName("  hAtFiElD  ")
        assertEquals(AreaPresets.Hatfield, found)

        val fallback = AreaPresets.byName("Unknown Area")
        assertEquals(AreaPresets.default, fallback)

        val nullFallback = AreaPresets.byName(null)
        assertEquals(AreaPresets.default, nullFallback)
    }
}
