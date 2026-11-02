package com.karabotshivhase.vibe.data.favorites

import kotlinx.coroutines.CompletableDeferred

class FakeFavoriteRemote : FavoriteRemote {
    var fetchAllResult: RemoteResult<List<RemoteFavorite>> = RemoteResult.Success(emptyList())
    var addResult: RemoteResult<RemoteFavorite> = RemoteResult.OtherError
    var removeResult: RemoteResult<Unit> = RemoteResult.Success(Unit)

    val callLog = mutableListOf<String>()

    var suspendGate: CompletableDeferred<Unit>? = null

    override suspend fun fetchAll(): RemoteResult<List<RemoteFavorite>> {
        callLog.add("fetchAll")
        suspendGate?.await()
        return fetchAllResult
    }

    override suspend fun add(venueId: Int): RemoteResult<RemoteFavorite> {
        callLog.add("add:$venueId")
        suspendGate?.await()
        return addResult
    }

    override suspend fun remove(venueId: Int): RemoteResult<Unit> {
        callLog.add("remove:$venueId")
        suspendGate?.await()
        return removeResult
    }
}
