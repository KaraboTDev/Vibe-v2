package com.karabotshivhase.vibe.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCodesTest {

    @Test
    fun normalize_null_or_unknown_returns_en() {
        assertEquals("en", LanguageCodes.normalize(null))
        assertEquals("en", LanguageCodes.normalize(""))
        assertEquals("en", LanguageCodes.normalize("   "))
        assertEquals("en", LanguageCodes.normalize("fr"))
    }

    @Test
    fun normalize_trims_and_is_case_insensitive() {
        assertEquals("zu", LanguageCodes.normalize("  ZU  "))
        assertEquals("af", LanguageCodes.normalize("AF"))
        assertEquals("en", LanguageCodes.normalize("En"))
    }

    @Test
    fun isSupported_only_en_zu_af() {
        assertTrue(LanguageCodes.isSupported("en"))
        assertTrue(LanguageCodes.isSupported("ZU"))
        assertTrue(LanguageCodes.isSupported(" af "))
        assertFalse(LanguageCodes.isSupported("fr"))
        assertFalse(LanguageCodes.isSupported(null))
    }
}
