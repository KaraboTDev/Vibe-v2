package com.karabotshivhase.vibe.data.settings

import com.karabotshivhase.vibe.data.favorites.RemoteResult

class FakeSettingsRemote : SettingsRemote {
    var fetchResult: RemoteResult<RemoteSettings> = RemoteResult.Success(RemoteSettings("en", null, true))
    var updateResult: RemoteResult<RemoteSettings> = RemoteResult.Success(RemoteSettings("en", null, true))

    val callLog = mutableListOf<String>()
    val lastUpdatePayload = mutableListOf<RemoteSettings>()

    override suspend fun fetch(): RemoteResult<RemoteSettings> {
        callLog.add("fetch")
        return fetchResult
    }

    override suspend fun update(settings: RemoteSettings): RemoteResult<RemoteSettings> {
        callLog.add("update")
        lastUpdatePayload.add(settings)
        return updateResult
    }
}
