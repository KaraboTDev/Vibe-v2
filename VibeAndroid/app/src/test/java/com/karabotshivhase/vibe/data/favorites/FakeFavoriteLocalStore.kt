package com.karabotshivhase.vibe.data.favorites

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFavoriteLocalStore : FavoriteLocalStore {
    val rows = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    override fun observeVisible(userId: Int): Flow<List<FavoriteEntity>> {
        return rows.map { list ->
            list.filter { it.userId == userId && it.syncState != SyncState.PENDING_REMOVE }
                .sortedByDescending { it.savedAt }
        }
    }

    override fun observeIsSaved(userId: Int, venueId: Int): Flow<Boolean> {
        return rows.map { list ->
            list.any { it.userId == userId && it.venueId == venueId && it.syncState != SyncState.PENDING_REMOVE }
        }
    }

    override fun observePendingCount(userId: Int): Flow<Int> {
        return rows.map { list ->
            list.count { it.userId == userId && (it.syncState == SyncState.PENDING_ADD || it.syncState == SyncState.PENDING_REMOVE) }
        }
    }

    override suspend fun getAll(userId: Int): List<FavoriteEntity> {
        return rows.value.filter { it.userId == userId }
    }

    override suspend fun get(userId: Int, venueId: Int): FavoriteEntity? {
        return rows.value.find { it.userId == userId && it.venueId == venueId }
    }

    override suspend fun upsert(entity: FavoriteEntity) {
        val current = rows.value.toMutableList()
        current.removeAll { it.userId == entity.userId && it.venueId == entity.venueId }
        current.add(entity)
        rows.value = current
    }

    override suspend fun delete(userId: Int, venueId: Int) {
        val current = rows.value.toMutableList()
        current.removeAll { it.userId == userId && it.venueId == venueId }
        rows.value = current
    }
}
