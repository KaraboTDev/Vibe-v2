package com.karabotshivhase.vibe.data.favorites

import com.karabotshivhase.vibe.venue.VenueDetail
import kotlinx.coroutines.flow.Flow

interface SavedSpaces {
    fun observeSaved(): Flow<List<SavedSpace>>
    fun observeIsSaved(venueId: Int): Flow<Boolean>
    fun observePendingCount(): Flow<Int>
    suspend fun save(detail: VenueDetail)
    suspend fun remove(venueId: Int)
    suspend fun getSaved(venueId: Int): SavedSpace?
}
