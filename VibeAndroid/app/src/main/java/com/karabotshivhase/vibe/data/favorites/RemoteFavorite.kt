package com.karabotshivhase.vibe.data.favorites

data class RemoteFavorite(
    val venueId: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val openingHours: String?,
    val tags: List<String>,
    val savedAtMillis: Long,
)
