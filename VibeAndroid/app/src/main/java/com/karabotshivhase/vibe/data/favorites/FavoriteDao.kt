package com.karabotshivhase.vibe.data.favorites

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId AND syncState != 'PENDING_REMOVE' ORDER BY savedAt DESC")
    fun observeVisible(userId: Int): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND venueId = :venueId AND syncState != 'PENDING_REMOVE')")
    fun observeIsSaved(userId: Int, venueId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND (syncState = 'PENDING_ADD' OR syncState = 'PENDING_REMOVE')")
    fun observePendingCount(userId: Int): Flow<Int>

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    suspend fun getAll(userId: Int): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND venueId = :venueId")
    suspend fun get(userId: Int, venueId: Int): FavoriteEntity?

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND venueId = :venueId")
    suspend fun delete(userId: Int, venueId: Int)
}
