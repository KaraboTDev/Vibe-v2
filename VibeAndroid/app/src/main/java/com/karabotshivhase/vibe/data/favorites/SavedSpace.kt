package com.karabotshivhase.vibe.data.favorites

data class SavedSpace(
    val venueId: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val openingHours: String?,
    val tags: List<String>,
    val savedAt: Long,
    val isPendingSync: Boolean,
)

fun FavoriteEntity.toSavedSpace(): SavedSpace {
    return SavedSpace(
        venueId = venueId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        openingHours = openingHours,
        tags = TagsCodec.decode(tags),
        savedAt = savedAt,
        isPendingSync = (syncState == SyncState.PENDING_ADD || syncState == SyncState.PENDING_REMOVE),
    )
}
