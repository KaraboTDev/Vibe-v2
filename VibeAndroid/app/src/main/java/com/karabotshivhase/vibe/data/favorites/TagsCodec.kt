package com.karabotshivhase.vibe.data.favorites

object TagsCodec {
    private const val SEPARATOR = "\u001F"

    fun encode(tags: List<String>): String {
        if (tags.isEmpty()) return ""
        return tags.joinToString(SEPARATOR)
    }

    fun decode(encoded: String?): List<String> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split(SEPARATOR)
    }
}
