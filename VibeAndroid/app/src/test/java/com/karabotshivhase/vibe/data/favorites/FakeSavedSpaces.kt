package com.karabotshivhase.vibe.data.favorites

import com.karabotshivhase.vibe.venue.VenueDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSavedSpaces : SavedSpaces {
    val savedList = MutableStateFlow<List<SavedSpace>>(emptyList())
    val pendingCountFlow = MutableStateFlow(0)

    val saveCallLog = mutableListOf<VenueDetail>()
    val removeCallLog = mutableListOf<Int>()

    override fun observeSaved(): Flow<List<SavedSpace>> = savedList

    override fun observeIsSaved(venueId: Int): Flow<Boolean> {
        return savedList.map { list ->
            list.any { it.venueId == venueId }
        }
    }

    override fun observePendingCount(): Flow<Int> = pendingCountFlow

    override suspend fun save(detail: VenueDetail) {
        saveCallLog.add(detail)
        val current = savedList.value.toMutableList()
        current.removeAll { it.venueId == detail.id }
        current.add(
            SavedSpace(
                venueId = detail.id,
                name = detail.name,
                latitude = detail.latitude,
                longitude = detail.longitude,
                address = detail.address,
                openingHours = detail.openingHours,
                tags = detail.tags.map { it.name },
                savedAt = System.currentTimeMillis(),
                isPendingSync = false,
            )
        )
        savedList.value = current
    }

    override suspend fun remove(venueId: Int) {
        removeCallLog.add(venueId)
        val current = savedList.value.toMutableList()
        current.removeAll { it.venueId == venueId }
        savedList.value = current
    }

    override suspend fun getSaved(venueId: Int): SavedSpace? {
        return savedList.value.find { it.venueId == venueId }
    }
}
