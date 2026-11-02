package com.karabotshivhase.vibe.data.favorites

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.venue.TagVote
import com.karabotshivhase.vibe.venue.VenueDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeLocal: FakeFavoriteLocalStore
    private lateinit var fakeScheduler: FakeSyncScheduler
    private lateinit var repository: FavoriteRepository
    private var currentUserId: Int? = 42

    private val sampleDetail = VenueDetail(
        id = 10,
        name = "Coffee Shop",
        latitude = -25.7,
        longitude = 28.2,
        address = "123 Main St",
        openingHours = "08:00-17:00",
        tags = listOf(TagVote("Quiet", 5)),
    )

    @Before
    fun setUp() {
        fakeLocal = FakeFavoriteLocalStore()
        fakeScheduler = FakeSyncScheduler()
        currentUserId = 42
        repository = FavoriteRepository(
            local = fakeLocal,
            userIdProvider = { currentUserId },
            scheduler = fakeScheduler,
            clock = { 1000L },
        )
    }

    @Test
    fun save_new_space_creates_pending_add_row_and_requests_sync() = runTest {
        repository.save(sampleDetail)

        val entity = fakeLocal.get(42, 10)
        assertNotNull(entity)
        assertEquals(SyncState.PENDING_ADD, entity!!.syncState)
        assertEquals(1000L, entity.savedAt)
        assertEquals(1, fakeScheduler.requestSyncCallCount)
    }

    @Test
    fun save_twice_keeps_single_row() = runTest {
        repository.save(sampleDetail)
        repository.save(sampleDetail)

        val allRows = fakeLocal.getAll(42)
        assertEquals(1, allRows.size)
        assertEquals(1, fakeScheduler.requestSyncCallCount)
    }

    @Test
    fun save_when_pending_remove_restores_synced() = runTest {
        val entity = FavoriteEntity(
            userId = 42,
            venueId = 10,
            name = "Coffee Shop",
            latitude = -25.7,
            longitude = 28.2,
            address = "123 Main St",
            openingHours = "08:00-17:00",
            tags = "Quiet",
            savedAt = 500L,
            syncState = SyncState.PENDING_REMOVE,
        )
        fakeLocal.upsert(entity)

        repository.save(sampleDetail)

        val updated = fakeLocal.get(42, 10)
        assertNotNull(updated)
        assertEquals(SyncState.SYNCED, updated!!.syncState)
        assertEquals(500L, updated.savedAt) // keeps savedAt
    }

    @Test
    fun remove_pending_add_deletes_row_and_does_not_request_sync() = runTest {
        repository.save(sampleDetail)
        val initialSyncCalls = fakeScheduler.requestSyncCallCount

        repository.remove(10)

        val entity = fakeLocal.get(42, 10)
        assertNull(entity)
        assertEquals(initialSyncCalls, fakeScheduler.requestSyncCallCount)
    }

    @Test
    fun remove_synced_marks_pending_remove_and_requests_sync() = runTest {
        val syncedEntity = FavoriteEntity(
            userId = 42,
            venueId = 10,
            name = "Coffee Shop",
            latitude = -25.7,
            longitude = 28.2,
            address = "123 Main St",
            openingHours = "08:00-17:00",
            tags = "Quiet",
            savedAt = 500L,
            syncState = SyncState.SYNCED,
        )
        fakeLocal.upsert(syncedEntity)

        repository.remove(10)

        val updated = fakeLocal.get(42, 10)
        assertNotNull(updated)
        assertEquals(SyncState.PENDING_REMOVE, updated!!.syncState)
        assertEquals(1, fakeScheduler.requestSyncCallCount)
    }

    @Test
    fun observeSaved_hides_pending_remove_and_other_users() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 1, "Mine", 0.0, 0.0, null, null, "", 100L, SyncState.SYNCED)
        )
        fakeLocal.upsert(
            FavoriteEntity(42, 2, "Removing", 0.0, 0.0, null, null, "", 200L, SyncState.PENDING_REMOVE)
        )
        fakeLocal.upsert(
            FavoriteEntity(99, 3, "Other User", 0.0, 0.0, null, null, "", 300L, SyncState.SYNCED)
        )

        val savedList = repository.observeSaved().first()
        assertEquals(1, savedList.size)
        assertEquals(1, savedList[0].venueId)
    }

    @Test
    fun observePendingCount_counts_pending_add_and_remove() = runTest {
        fakeLocal.upsert(
            FavoriteEntity(42, 1, "Add", 0.0, 0.0, null, null, "", 100L, SyncState.PENDING_ADD)
        )
        fakeLocal.upsert(
            FavoriteEntity(42, 2, "Remove", 0.0, 0.0, null, null, "", 200L, SyncState.PENDING_REMOVE)
        )
        fakeLocal.upsert(
            FavoriteEntity(42, 3, "Synced", 0.0, 0.0, null, null, "", 300L, SyncState.SYNCED)
        )

        val pendingCount = repository.observePendingCount().first()
        assertEquals(2, pendingCount)
    }

    @Test
    fun operations_do_nothing_when_logged_out() = runTest {
        currentUserId = null

        repository.save(sampleDetail)
        assertEquals(0, fakeLocal.rows.value.size)

        val savedList = repository.observeSaved().first()
        assertTrue(savedList.isEmpty())

        val isSaved = repository.observeIsSaved(10).first()
        assertFalse(isSaved)

        val pendingCount = repository.observePendingCount().first()
        assertEquals(0, pendingCount)
    }
}
