package com.karabotshivhase.vibe.data.favorites

import com.karabotshivhase.vibe.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteSyncerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeLocal: FakeFavoriteLocalStore
    private lateinit var fakeRemote: FakeFavoriteRemote
    private lateinit var syncer: FavoriteSyncer
    private var currentUserId: Int? = 42

    @Before
    fun setUp() {
        fakeLocal = FakeFavoriteLocalStore()
        fakeRemote = FakeFavoriteRemote()
        currentUserId = 42
        syncer = FavoriteSyncer(
            local = fakeLocal,
            remote = fakeRemote,
            userIdProvider = { currentUserId },
        )
    }

    @Test
    fun push_add_success_marks_synced_with_server_data() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 10, "Local Name", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        val serverFav = RemoteFavorite(10, "Server Name", -25.0, 28.0, "Server Addr", "08-17", listOf("Quiet"), 500L)
        fakeRemote.addResult = RemoteResult.Success(serverFav)
        fakeRemote.fetchAllResult = RemoteResult.Success(listOf(serverFav))

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Done, outcome)
        val updated = fakeLocal.get(42, 10)
        assertNotNull(updated)
        assertEquals(SyncState.SYNCED, updated!!.syncState)
        assertEquals("Server Name", updated.name)
        assertEquals(500L, updated.savedAt)
    }

    @Test
    fun push_add_404_deletes_local_row() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 10, "Deleted Venue", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        fakeRemote.addResult = RemoteResult.NotFound

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Done, outcome)
        assertNull(fakeLocal.get(42, 10))
    }

    @Test
    fun push_remove_success_deletes_row() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 10, "Venue", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_REMOVE)
        )
        fakeRemote.removeResult = RemoteResult.Success(Unit)

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Done, outcome)
        assertNull(fakeLocal.get(42, 10))
    }

    @Test
    fun push_network_error_keeps_pending_and_returns_Offline() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 10, "Local Name", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        fakeRemote.addResult = RemoteResult.NetworkError

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Offline, outcome)
        val entity = fakeLocal.get(42, 10)
        assertNotNull(entity)
        assertEquals(SyncState.PENDING_ADD, entity!!.syncState)
    }

    @Test
    fun push_unauthorized_returns_SessionExpired_and_keeps_pending() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 10, "Local Name", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        fakeRemote.addResult = RemoteResult.Unauthorized

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.SessionExpired, outcome)
        val entity = fakeLocal.get(42, 10)
        assertNotNull(entity)
        assertEquals(SyncState.PENDING_ADD, entity!!.syncState)
    }

    @Test
    fun pull_inserts_missing_server_favorites_as_synced() = runTest {
        val serverFav = RemoteFavorite(20, "Server Venue", -25.0, 28.0, "Addr", "08-17", listOf("Focus"), 300L)
        fakeRemote.fetchAllResult = RemoteResult.Success(listOf(serverFav))

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Done, outcome)
        val entity = fakeLocal.get(42, 20)
        assertNotNull(entity)
        assertEquals(SyncState.SYNCED, entity!!.syncState)
        assertEquals("Server Venue", entity.name)
    }

    @Test
    fun pull_deletes_synced_rows_missing_on_server() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 30, "Removed Elsewhere", 0.0, 0.0, null, null, "", 100L, SyncState.SYNCED)
        )
        fakeRemote.fetchAllResult = RemoteResult.Success(emptyList())

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Done, outcome)
        assertNull(fakeLocal.get(42, 30))
    }

    @Test
    fun pull_never_overwrites_pending_rows() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 40, "Pending Local", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        val serverFav = RemoteFavorite(40, "Server Version", -25.0, 28.0, "Addr", "08-17", listOf("Focus"), 300L)
        fakeRemote.fetchAllResult = RemoteResult.Success(listOf(serverFav))

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Done, outcome)
        val entity = fakeLocal.get(42, 40)
        assertNotNull(entity)
        assertEquals(SyncState.PENDING_ADD, entity!!.syncState)
        assertEquals("Pending Local", entity.name)
    }

    @Test
    fun push_happens_before_pull() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 10, "Local", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        fakeRemote.addResult = RemoteResult.Success(
            RemoteFavorite(10, "Server", 0.0, 0.0, null, null, emptyList(), 100L)
        )

        syncer.sync()

        assertEquals(2, fakeRemote.callLog.size)
        assertEquals("add:10", fakeRemote.callLog[0])
        assertEquals("fetchAll", fakeRemote.callLog[1])
    }

    @Test
    fun second_sync_while_first_is_running_returns_Skipped() = runTest {
        val gate = CompletableDeferred<Unit>()
        fakeRemote.suspendGate = gate

        var firstOutcome: SyncOutcome? = null
        val firstJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            firstOutcome = syncer.sync()
        }

        // Try launching second sync while first is waiting on gate
        val secondOutcome = syncer.sync()
        assertEquals(SyncOutcome.Skipped, secondOutcome)

        gate.complete(Unit)
        firstJob.join()
        assertEquals(SyncOutcome.Done, firstOutcome)
    }

    @Test
    fun sync_returns_Skipped_when_logged_out() = runTest {
        currentUserId = null

        val outcome = syncer.sync()

        assertEquals(SyncOutcome.Skipped, outcome)
        assertEquals(0, fakeRemote.callLog.size)
    }
}
