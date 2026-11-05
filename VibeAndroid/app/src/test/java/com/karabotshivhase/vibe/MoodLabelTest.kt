package com.karabotshivhase.vibe

import com.karabotshivhase.vibe.util.moodLabelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoodLabelTest {

    @Test
    fun moodLabelRes_maps_all_five_server_tags_case_insensitively() {
        assertEquals(R.string.mood_quiet, moodLabelRes("Quiet"))
        assertEquals(R.string.mood_quiet, moodLabelRes("quiet"))
        assertEquals(R.string.mood_social, moodLabelRes("Social"))
        assertEquals(R.string.mood_social, moodLabelRes("social"))
        assertEquals(R.string.mood_focus, moodLabelRes("Focus"))
        assertEquals(R.string.mood_focus, moodLabelRes("focus"))
        assertEquals(R.string.mood_creative, moodLabelRes("Creative"))
        assertEquals(R.string.mood_creative, moodLabelRes("creative"))
        assertEquals(R.string.mood_cheap_wifi, moodLabelRes("Cheap Wifi"))
        assertEquals(R.string.mood_cheap_wifi, moodLabelRes("cheapwifi"))
        assertEquals(R.string.mood_cheap_wifi, moodLabelRes("  Cheap Wifi  "))
    }

    @Test
    fun moodLabelRes_returns_null_for_unknown_name() {
        assertNull(moodLabelRes(null))
        assertNull(moodLabelRes(""))
        assertNull(moodLabelRes("   "))
        assertNull(moodLabelRes("Unknown Mood"))
    }
}
