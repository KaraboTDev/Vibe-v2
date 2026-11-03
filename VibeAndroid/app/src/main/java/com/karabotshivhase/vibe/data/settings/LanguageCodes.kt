package com.karabotshivhase.vibe.data.settings

object LanguageCodes {
    val SUPPORTED = listOf("en", "zu", "af")

    fun normalize(code: String?): String {
        if (code.isNullOrBlank()) return "en"
        val cleaned = code.trim().lowercase()
        return if (cleaned in SUPPORTED) cleaned else "en"
    }

    fun isSupported(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        return code.trim().lowercase() in SUPPORTED
    }
}
