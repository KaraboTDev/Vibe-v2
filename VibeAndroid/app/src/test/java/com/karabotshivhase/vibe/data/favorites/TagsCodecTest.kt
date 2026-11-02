package com.karabotshivhase.vibe.data.favorites

import org.junit.Assert.assertEquals
import org.junit.Test

class TagsCodecTest {

    @Test
    fun roundtrip_preserves_order() {
        val original = listOf("Quiet", "Social", "Focus")
        val encoded = TagsCodec.encode(original)
        val decoded = TagsCodec.decode(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun blank_or_null_decodes_to_empty_list() {
        assertEquals(emptyList<String>(), TagsCodec.decode(null))
        assertEquals(emptyList<String>(), TagsCodec.decode(""))
        assertEquals(emptyList<String>(), TagsCodec.decode("   "))
    }

    @Test
    fun empty_list_encodes_to_empty_string() {
        assertEquals("", TagsCodec.encode(emptyList()))
    }
}
