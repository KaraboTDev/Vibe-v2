package com.karabotshivhase.vibe.network

data class TagDto(
    val id: Int?,
    val name: String?,
)

data class VenueSummaryDto(
    val id: Int?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val openingHours: String?,
    val tags: List<String>?,
    val distanceMeters: Int?,
)

data class TagVoteDto(
    val name: String?,
    val voteCount: Int?,
)

data class VenueDetailDto(
    val id: Int?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val openingHours: String?,
    val tags: List<TagVoteDto>?,
)
