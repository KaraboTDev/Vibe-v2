package com.karabotshivhase.vibe.util

import java.util.Locale

object DistanceFormatter {
    fun format(meters: Int, locale: Locale = Locale.getDefault()): String {
        if (meters <= 0) return "0 m"
        if (meters < 1000) {
            return String.format(locale, "%d m", meters)
        }
        val km = meters / 1000.0
        return String.format(locale, "%.1f km", km)
    }
}
