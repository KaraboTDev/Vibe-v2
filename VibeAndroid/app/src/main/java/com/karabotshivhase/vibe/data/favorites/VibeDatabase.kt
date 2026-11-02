package com.karabotshivhase.vibe.data.favorites

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
@TypeConverters(SyncStateConverter::class)
abstract class VibeDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
