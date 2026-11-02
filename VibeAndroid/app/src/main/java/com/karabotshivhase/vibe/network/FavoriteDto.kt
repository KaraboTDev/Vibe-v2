package com.karabotshivhase.vibe.network

data class FavoriteDto(
    val venueId: Int?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val openingHours: String?,
    val tags: List<String>?,
    val savedAt: String?,
)
