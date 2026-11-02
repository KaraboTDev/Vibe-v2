package com.karabotshivhase.vibe.venue

data class Tag(
    val id: Int,
    val name: String,
)

data class Venue(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val openingHours: String?,
    val tags: List<String>,
    val distanceMeters: Int,
)

data class TagVote(
    val name: String,
    val voteCount: Int,
)

data class VenueDetail(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val openingHours: String?,
    val tags: List<TagVote>,
)

enum class LoadFailure {
    Network,
    NotFound,
    InvalidInput,
    Unknown,
}

sealed interface LoadResult<out T> {
    data class Success<out T>(val data: T) : LoadResult<T>
    data class Failure(val reason: LoadFailure) : LoadResult<Nothing>
}

interface VenueSource {
    suspend fun getTags(): LoadResult<List<Tag>>
    suspend fun search(
        mood: String,
        areaLat: Double,
        areaLng: Double,
        radiusKm: Double,
        limit: Int,
    ): LoadResult<List<Venue>>
    suspend fun getVenue(id: Int): LoadResult<VenueDetail>
}
