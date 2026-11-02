package com.karabotshivhase.vibe.util

import com.karabotshivhase.vibe.R

fun moodLabelRes(serverName: String?): Int? {
    if (serverName.isNullOrBlank()) return null
    return when (serverName.trim().lowercase()) {
        "quiet" -> R.string.mood_quiet
        "social" -> R.string.mood_social
        "focus" -> R.string.mood_focus
        "creative" -> R.string.mood_creative
        "cheap wifi", "cheapwifi" -> R.string.mood_cheap_wifi
        else -> null
    }
}
