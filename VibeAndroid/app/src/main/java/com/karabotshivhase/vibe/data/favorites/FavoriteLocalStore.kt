package com.karabotshivhase.vibe.data.favorites

import kotlinx.coroutines.flow.Flow

interface FavoriteLocalStore {
    fun observeVisible(userId: Int): Flow<List<FavoriteEntity>>
    fun observeIsSaved(userId: Int, venueId: Int): Flow<Boolean>
    fun observePendingCount(userId: Int): Flow<Int>
    suspend fun getAll(userId: Int): List<FavoriteEntity>
    suspend fun get(userId: Int, venueId: Int): FavoriteEntity?
    suspend fun upsert(entity: FavoriteEntity)
    suspend fun delete(userId: Int, venueId: Int)
}

class RoomFavoriteLocalStore(
    private val dao: FavoriteDao,
) : FavoriteLocalStore {
    override fun observeVisible(userId: Int): Flow<List<FavoriteEntity>> = dao.observeVisible(userId)
    override fun observeIsSaved(userId: Int, venueId: Int): Flow<Boolean> = dao.observeIsSaved(userId, venueId)
    override fun observePendingCount(userId: Int): Flow<Int> = dao.observePendingCount(userId)
    override suspend fun getAll(userId: Int): List<FavoriteEntity> = dao.getAll(userId)
    override suspend fun get(userId: Int, venueId: Int): FavoriteEntity? = dao.get(userId, venueId)
    override suspend fun upsert(entity: FavoriteEntity) {
        dao.upsert(entity)
    }
    override suspend fun delete(userId: Int, venueId: Int) {
        dao.delete(userId, venueId)
    }
}
