package com.karabotshivhase.vibe.ui.settings

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.data.settings.FakeSettingsSource
import com.karabotshivhase.vibe.data.settings.SettingsChange
import com.karabotshivhase.vibe.data.settings.UpdateResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSource: FakeSettingsSource
    private lateinit var fakeSession: FakeSessionStore
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakeSource = FakeSettingsSource()
        fakeSession = FakeSessionStore()
    }

    @Test
    fun exposes_current_settings_initially() = runTest {
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        val state = viewModel.uiState.value
        assertEquals("en", state.language)
        assertEquals("Hatfield", state.areaName)
        assertTrue(state.notificationsEnabled)
        assertEquals("user@vibe.app", state.email)
    }

    @Test
    fun selecting_language_updates_state_and_emits_ApplyLanguage() = runTest {
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        var emittedEvent: SettingsEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { emittedEvent = it }
        }

        viewModel.selectLanguage("zu")

        assertEquals("zu", viewModel.uiState.value.language)
        assertEquals(R.string.settings_saved, viewModel.uiState.value.messageRes)
        assertEquals(SettingsEvent.ApplyLanguage("zu"), emittedEvent)
    }

    @Test
    fun change_saved_offline_shows_saved_on_device_message() = runTest {
        fakeSource.updateResultToReturn = UpdateResult.SavedLocally
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        viewModel.selectArea("Menlyn")

        assertEquals("Menlyn", viewModel.uiState.value.areaName)
        assertEquals(R.string.settings_saved_offline, viewModel.uiState.value.messageRes)
    }

    @Test
    fun session_expired_emits_SessionExpired_event() = runTest {
        fakeSource.updateResultToReturn = UpdateResult.SessionExpired
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        var emittedEvent: SettingsEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { emittedEvent = it }
        }

        viewModel.setNotifications(false)

        assertEquals(SettingsEvent.SessionExpired, emittedEvent)
    }

    @Test
    fun invalid_result_shows_invalid_message() = runTest {
        fakeSource.updateResultToReturn = UpdateResult.InvalidInput
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        viewModel.selectLanguage("invalid")

        assertEquals(R.string.settings_invalid, viewModel.uiState.value.messageRes)
    }

    @Test
    fun toggling_notifications_calls_repository() = runTest {
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        viewModel.setNotifications(false)

        assertEquals(1, fakeSource.updateCallLog.size)
        assertEquals(SettingsChange.Notifications(false), fakeSource.updateCallLog[0])
        assertFalse(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun logout_clears_session_and_settings_and_emits_NavigateLogin() = runTest {
        viewModel = SettingsViewModel(fakeSource, fakeSession, "user@vibe.app")

        var emittedEvent: SettingsEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { emittedEvent = it }
        }

        viewModel.logout()

        assertEquals(1, fakeSession.clearCallCount)
        assertEquals(1, fakeSource.clearCallCount)
        assertEquals(SettingsEvent.NavigateLogin, emittedEvent)
    }
}
