package com.karabotshivhase.vibe

import android.app.Application
import androidx.room.Room
import com.karabotshivhase.vibe.auth.AuthRepository
import com.karabotshivhase.vibe.auth.TokenStore
import com.karabotshivhase.vibe.data.favorites.ApiFavoriteRemote
import com.karabotshivhase.vibe.data.favorites.FavoriteLocalStore
import com.karabotshivhase.vibe.data.favorites.FavoriteRemote
import com.karabotshivhase.vibe.data.favorites.FavoriteRepository
import com.karabotshivhase.vibe.data.favorites.FavoriteSyncer
import com.karabotshivhase.vibe.data.favorites.RoomFavoriteLocalStore
import com.karabotshivhase.vibe.data.favorites.SyncScheduler
import com.karabotshivhase.vibe.data.favorites.VibeDatabase
import com.karabotshivhase.vibe.data.favorites.WorkManagerSyncScheduler
import com.karabotshivhase.vibe.network.ApiClient
import com.karabotshivhase.vibe.network.ApiConfig
import com.karabotshivhase.vibe.network.VibeApi
import com.karabotshivhase.vibe.venue.VenueRepository

class VibeApp : Application() {
    val tokenStore: TokenStore by lazy {
        TokenStore(this)
    }

    val api: VibeApi by lazy {
        ApiClient.createApi(
            baseUrl = ApiConfig.BASE_URL,
            tokenProvider = { tokenStore.token() },
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(api = api, session = tokenStore)
    }

    val venueRepository: VenueRepository by lazy {
        VenueRepository(api = api)
    }

    val database: VibeDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            VibeDatabase::class.java,
            "vibe.db"
        ).build()
    }

    val localStore: FavoriteLocalStore by lazy {
        RoomFavoriteLocalStore(database.favoriteDao())
    }

    val remote: FavoriteRemote by lazy {
        ApiFavoriteRemote(api)
    }

    val scheduler: SyncScheduler by lazy {
        WorkManagerSyncScheduler(this)
    }

    val repository: FavoriteRepository by lazy {
        FavoriteRepository(
            local = localStore,
            userIdProvider = { tokenStore.userId() },
            scheduler = scheduler,
        )
    }

    val syncer: FavoriteSyncer by lazy {
        FavoriteSyncer(
            local = localStore,
            remote = remote,
            userIdProvider = { tokenStore.userId() },
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (tokenStore.isLoggedIn()) {
            scheduler.requestSync()
        }
    }
}
