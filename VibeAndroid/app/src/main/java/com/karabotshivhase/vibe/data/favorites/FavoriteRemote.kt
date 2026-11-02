package com.karabotshivhase.vibe.data.favorites

interface FavoriteRemote {
    suspend fun fetchAll(): RemoteResult<List<RemoteFavorite>>
    suspend fun add(venueId: Int): RemoteResult<RemoteFavorite>
    suspend fun remove(venueId: Int): RemoteResult<Unit>
}
