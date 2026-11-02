package com.karabotshivhase.vibe.data.favorites

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

class FavoriteSyncer(
    private val local: FavoriteLocalStore,
    private val remote: FavoriteRemote,
    private val userIdProvider: () -> Int?,
) : SyncRunner {

    private val mutex = Mutex()

    override suspend fun sync(): SyncOutcome {
        val userId = userIdProvider() ?: return SyncOutcome.Skipped
        if (!mutex.tryLock()) {
            return SyncOutcome.Skipped
        }

        try {
            // a) PUSH
            val allRows = local.getAll(userId)
            val pendingAdds = allRows.filter { it.syncState == SyncState.PENDING_ADD }.sortedBy { it.savedAt }
            val pendingRemoves = allRows.filter { it.syncState == SyncState.PENDING_REMOVE }

            for (row in pendingAdds) {
                when (val result = remote.add(row.venueId)) {
                    is RemoteResult.Success -> {
                        val remoteFav = result.value
                        val syncedEntity = FavoriteEntity(
                            userId = userId,
                            venueId = remoteFav.venueId,
                            name = remoteFav.name,
                            latitude = remoteFav.latitude,
                            longitude = remoteFav.longitude,
                            address = remoteFav.address,
                            openingHours = remoteFav.openingHours,
                            tags = TagsCodec.encode(remoteFav.tags),
                            savedAt = remoteFav.savedAtMillis,
                            syncState = SyncState.SYNCED,
                        )
                        local.upsert(syncedEntity)
                    }
                    is RemoteResult.NotFound -> {
                        local.delete(userId, row.venueId)
                    }
                    is RemoteResult.NetworkError -> {
                        return SyncOutcome.Offline
                    }
                    is RemoteResult.Unauthorized -> {
                        return SyncOutcome.SessionExpired
                    }
                    is RemoteResult.OtherError -> {
                        // leave pending, continue
                    }
                }
            }

            for (row in pendingRemoves) {
                when (remote.remove(row.venueId)) {
                    is RemoteResult.Success,
                    is RemoteResult.NotFound,
                    -> {
                        local.delete(userId, row.venueId)
                    }
                    is RemoteResult.NetworkError -> {
                        return SyncOutcome.Offline
                    }
                    is RemoteResult.Unauthorized -> {
                        return SyncOutcome.SessionExpired
                    }
                    is RemoteResult.OtherError -> {
                        // leave pending, continue
                    }
                }
            }

            // b) PULL
            when (val fetchResult = remote.fetchAll()) {
                is RemoteResult.Success -> {
                    val serverFavs = fetchResult.value
                    val serverVenueIds = serverFavs.map { it.venueId }.toSet()
                    val currentLocalRows = local.getAll(userId)
                    val localMap = currentLocalRows.associateBy { it.venueId }

                    for (fav in serverFavs) {
                        val localRow = localMap[fav.venueId]
                        if (localRow == null) {
                            val entity = FavoriteEntity(
                                userId = userId,
                                venueId = fav.venueId,
                                name = fav.name,
                                latitude = fav.latitude,
                                longitude = fav.longitude,
                                address = fav.address,
                                openingHours = fav.openingHours,
                                tags = TagsCodec.encode(fav.tags),
                                savedAt = fav.savedAtMillis,
                                syncState = SyncState.SYNCED,
                            )
                            local.upsert(entity)
                        } else if (localRow.syncState == SyncState.SYNCED) {
                            val entity = localRow.copy(
                                name = fav.name,
                                latitude = fav.latitude,
                                longitude = fav.longitude,
                                address = fav.address,
                                openingHours = fav.openingHours,
                                tags = TagsCodec.encode(fav.tags),
                                savedAt = fav.savedAtMillis,
                            )
                            local.upsert(entity)
                        }
                        // NEVER touch PENDING_ADD or PENDING_REMOVE
                    }

                    // Delete local SYNCED rows not present on server
                    for (row in currentLocalRows) {
                        if (row.syncState == SyncState.SYNCED && row.venueId !in serverVenueIds) {
                            local.delete(userId, row.venueId)
                        }
                    }
                }
                is RemoteResult.NotFound -> {
                    return SyncOutcome.Failed
                }
                is RemoteResult.NetworkError -> {
                    return SyncOutcome.Offline
                }
                is RemoteResult.Unauthorized -> {
                    return SyncOutcome.SessionExpired
                }
                is RemoteResult.OtherError -> {
                    return SyncOutcome.Failed
                }
            }

            return SyncOutcome.Done
        } catch (e: CancellationException) {
            throw e
        } finally {
            mutex.unlock()
        }
    }
}
