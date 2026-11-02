package com.karabotshivhase.vibe.util

object HoursFormatter {
    fun clean(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val collapsed = trimmed.replace(Regex("\\s+"), " ")
        val cleaned = collapsed.replace(Regex("\\s+([,;])"), "$1")
        return if (cleaned.isBlank()) null else cleaned
    }
}
