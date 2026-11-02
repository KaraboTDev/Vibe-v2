package com.karabotshivhase.vibe.util

data class Area(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object AreaPresets {
    val PretoriaCentral = Area("Pretoria Central", -25.7461, 28.1881)
    val Hatfield = Area("Hatfield", -25.7486, 28.2385)
    val Brooklyn = Area("Brooklyn", -25.7685, 28.2403)
    val Menlyn = Area("Menlyn", -25.7826, 28.2767)

    val all: List<Area> = listOf(PretoriaCentral, Hatfield, Brooklyn, Menlyn)
    val default: Area = Hatfield

    fun byName(name: String?): Area {
        if (name.isNullOrBlank()) return default
        val trimmed = name.trim()
        return all.find { it.name.equals(trimmed, ignoreCase = true) } ?: default
    }
}
