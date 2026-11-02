package com.karabotshivhase.vibe.data.favorites

import com.karabotshivhase.vibe.venue.VenueDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepository(
    private val local: FavoriteLocalStore,
    private val userIdProvider: () -> Int?,
    private val scheduler: SyncScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun observeSaved(): Flow<List<SavedSpace>> {
        val userId = userIdProvider() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return local.observeVisible(userId).map { entities ->
            entities.map { it.toSavedSpace() }
        }
    }

    fun observeIsSaved(venueId: Int): Flow<Boolean> {
        val userId = userIdProvider() ?: return kotlinx.coroutines.flow.flowOf(false)
        return local.observeIsSaved(userId, venueId)
    }

    fun observePendingCount(): Flow<Int> {
        val userId = userIdProvider() ?: return kotlinx.coroutines.flow.flowOf(0)
        return local.observePendingCount(userId)
    }

    suspend fun save(detail: VenueDetail) {
        val userId = userIdProvider() ?: return
        val existing = local.get(userId, detail.id)
        val tagNames = detail.tags.map { it.name }
        val tagsEncoded = TagsCodec.encode(tagNames)

        if (existing != null) {
            if (existing.syncState == SyncState.PENDING_REMOVE) {
                val updated = existing.copy(
                    name = detail.name,
                    latitude = detail.latitude,
                    longitude = detail.longitude,
                    address = detail.address,
                    openingHours = detail.openingHours,
                    tags = tagsEncoded,
                    syncState = SyncState.SYNCED,
                )
                local.upsert(updated)
            }
            // If SYNCED or PENDING_ADD, change nothing
        } else {
            val entity = FavoriteEntity(
                userId = userId,
                venueId = detail.id,
                name = detail.name,
                latitude = detail.latitude,
                longitude = detail.longitude,
                address = detail.address,
                openingHours = detail.openingHours,
                tags = tagsEncoded,
                savedAt = clock(),
                syncState = SyncState.PENDING_ADD,
            )
            local.upsert(entity)
            scheduler.requestSync()
        }
    }

    suspend fun remove(venueId: Int) {
        val userId = userIdProvider() ?: return
        val existing = local.get(userId, venueId) ?: return

        when (existing.syncState) {
            SyncState.PENDING_ADD -> {
                local.delete(userId, venueId)
            }
            SyncState.SYNCED -> {
                val updated = existing.copy(syncState = SyncState.PENDING_REMOVE)
                local.upsert(updated)
                scheduler.requestSync()
            }
            SyncState.PENDING_REMOVE -> {
                // do nothing
            }
        }
    }
}
