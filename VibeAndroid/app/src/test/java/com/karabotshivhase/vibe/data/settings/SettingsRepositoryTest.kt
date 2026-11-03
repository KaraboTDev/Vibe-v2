package com.karabotshivhase.vibe.data.settings

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.data.favorites.RemoteResult
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeStore: FakeSettingsStore
    private lateinit var fakeRemote: FakeSettingsRemote
    private lateinit var repository: SettingsRepository
    private var currentUserId: Int? = 42

    @Before
    fun setUp() {
        fakeStore = FakeSettingsStore()
        fakeRemote = FakeSettingsRemote()
        currentUserId = 42
        repository = SettingsRepository(
            store = fakeStore,
            remote = fakeRemote,
            userIdProvider = { currentUserId },
        )
    }

    @Test
    fun update_language_saves_locally_and_syncs_to_server() = runTest {
        val result = repository.update(SettingsChange.Language("zu"))

        assertEquals(UpdateResult.Synced, result)
        assertEquals("zu", fakeStore.read().language)
        assertFalse(fakeStore.isDirty())
        assertEquals(1, fakeRemote.callLog.size)
        assertEquals("zu", fakeRemote.lastUpdatePayload[0].language)
    }

    @Test
    fun update_while_offline_keeps_change_and_marks_dirty() = runTest {
        fakeRemote.updateResult = RemoteResult.NetworkError

        val result = repository.update(SettingsChange.Language("af"))

        assertEquals(UpdateResult.SavedLocally, result)
        assertEquals("af", fakeStore.read().language)
        assertTrue(fakeStore.isDirty())
    }

    @Test
    fun update_unauthorized_returns_SessionExpired_and_keeps_local_change() = runTest {
        fakeRemote.updateResult = RemoteResult.Unauthorized

        val result = repository.update(SettingsChange.Notifications(false))

        assertEquals(UpdateResult.SessionExpired, result)
        assertFalse(fakeStore.read().notificationsEnabled)
        assertTrue(fakeStore.isDirty())
    }

    @Test
    fun update_unsupported_language_changes_nothing() = runTest {
        val result = repository.update(SettingsChange.Language("fr"))

        assertEquals(UpdateResult.InvalidInput, result)
        assertEquals("en", fakeStore.read().language)
        assertEquals(0, fakeRemote.callLog.size)
    }

    @Test
    fun update_unknown_area_changes_nothing() = runTest {
        val result = repository.update(SettingsChange.Area("Unknown Area"))

        assertEquals(UpdateResult.InvalidInput, result)
        assertEquals(null, fakeStore.read().defaultAreaName)
        assertEquals(0, fakeRemote.callLog.size)
    }

    @Test
    fun update_area_is_case_insensitive_and_stored_canonical() = runTest {
        val result = repository.update(SettingsChange.Area("  hAtFiElD  "))

        assertEquals(UpdateResult.Synced, result)
        assertEquals(AreaPresets.Hatfield.name, fakeStore.read().defaultAreaName)
    }

    @Test
    fun update_sends_full_state_not_only_the_changed_field() = runTest {
        fakeStore.write(UserSettings(language = "zu", defaultAreaName = AreaPresets.Brooklyn.name, notificationsEnabled = false))

        repository.update(SettingsChange.Notifications(true))

        assertEquals(1, fakeRemote.lastUpdatePayload.size)
        val payload = fakeRemote.lastUpdatePayload[0]
        assertEquals("zu", payload.language)
        assertEquals(AreaPresets.Brooklyn.name, payload.defaultArea)
        assertTrue(payload.notificationsEnabled)
    }

    @Test
    fun refresh_pushes_dirty_changes_before_pulling() = runTest {
        fakeStore.write(UserSettings(language = "af", defaultAreaName = AreaPresets.Menlyn.name, notificationsEnabled = true))
        fakeStore.setDirty(true)
        fakeRemote.updateResult = RemoteResult.Success(RemoteSettings("af", AreaPresets.Menlyn.name, true))
        fakeRemote.fetchResult = RemoteResult.Success(RemoteSettings("af", AreaPresets.Menlyn.name, true))

        val result = repository.refresh()

        assertEquals(2, fakeRemote.callLog.size)
        assertEquals("update", fakeRemote.callLog[0])
        assertEquals("fetch", fakeRemote.callLog[1])
        assertFalse(fakeStore.isDirty())
        assertEquals(RefreshResult.Unchanged, result)
    }

    @Test
    fun refresh_adopts_server_values_when_not_dirty() = runTest {
        fakeRemote.fetchResult = RemoteResult.Success(RemoteSettings("zu", AreaPresets.Brooklyn.name, false))

        val result = repository.refresh()

        assertTrue(result is RefreshResult.Updated)
        val updated = (result as RefreshResult.Updated).settings
        assertEquals("zu", updated.language)
        assertEquals(AreaPresets.Brooklyn.name, updated.defaultAreaName)
        assertFalse(updated.notificationsEnabled)
        assertEquals("zu", fakeStore.read().language)
    }

    @Test
    fun refresh_keeps_dirty_local_values_when_push_fails() = runTest {
        fakeStore.write(UserSettings(language = "af", defaultAreaName = null, notificationsEnabled = true))
        fakeStore.setDirty(true)
        fakeRemote.updateResult = RemoteResult.NetworkError

        val result = repository.refresh()

        assertEquals(RefreshResult.Offline, result)
        assertEquals("af", fakeStore.read().language)
        assertTrue(fakeStore.isDirty())
        assertEquals(1, fakeRemote.callLog.size) // Didn't pull
    }

    @Test
    fun refresh_offline_keeps_local_values() = runTest {
        fakeRemote.fetchResult = RemoteResult.NetworkError

        val result = repository.refresh()

        assertEquals(RefreshResult.Offline, result)
        assertEquals("en", fakeStore.read().language)
    }

    @Test
    fun refresh_unchanged_returns_Unchanged() = runTest {
        fakeRemote.fetchResult = RemoteResult.Success(RemoteSettings("en", null, true))

        val result = repository.refresh()

        assertEquals(RefreshResult.Unchanged, result)
    }

    @Test
    fun refresh_when_logged_out_returns_Skipped_and_makes_no_calls() = runTest {
        currentUserId = null

        val result = repository.refresh()

        assertEquals(RefreshResult.Skipped, result)
        assertEquals(0, fakeRemote.callLog.size)
    }

    @Test
    fun update_when_logged_out_returns_NotLoggedIn_and_changes_nothing() = runTest {
        currentUserId = null

        val result = repository.update(SettingsChange.Language("zu"))

        assertEquals(UpdateResult.NotLoggedIn, result)
        assertEquals(0, fakeRemote.callLog.size)
        assertEquals("en", fakeStore.read().language)
    }

    @Test
    fun clear_removes_cached_settings_and_dirty_flag() = runTest {
        fakeStore.write(UserSettings(language = "zu", defaultAreaName = AreaPresets.Menlyn.name, notificationsEnabled = false))
        fakeStore.setDirty(true)

        repository.clear()

        assertEquals("en", fakeStore.read().language)
        assertNull(fakeStore.read().defaultAreaName)
        assertTrue(fakeStore.read().notificationsEnabled)
        assertFalse(fakeStore.isDirty())
    }
}
