package com.karabotshivhase.vibe.data.favorites

import androidx.room.Entity
import androidx.room.TypeConverter

class SyncStateConverter {
    @TypeConverter
    fun fromSyncState(state: SyncState): String = state.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = try {
        SyncState.valueOf(value)
    } catch (_: Exception) {
        SyncState.SYNCED
    }
}

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "venueId"],
)
data class FavoriteEntity(
    val userId: Int,
    val venueId: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val openingHours: String?,
    val tags: String,
    val savedAt: Long,
    val syncState: SyncState,
)
