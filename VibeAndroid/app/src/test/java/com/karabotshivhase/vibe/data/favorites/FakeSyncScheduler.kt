package com.karabotshivhase.vibe.data.favorites

class FakeSyncScheduler : SyncScheduler {
    var requestSyncCallCount = 0

    override fun requestSync() {
        requestSyncCallCount++
    }
}
