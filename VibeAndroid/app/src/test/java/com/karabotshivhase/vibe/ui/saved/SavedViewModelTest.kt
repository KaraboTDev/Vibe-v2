package com.karabotshivhase.vibe.ui.saved

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.data.favorites.FakeSavedSpaces
import com.karabotshivhase.vibe.data.favorites.SavedSpace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSaved: FakeSavedSpaces
    private lateinit var viewModel: SavedViewModel

    @Before
    fun setUp() {
        fakeSaved = FakeSavedSpaces()
    }

    @Test
    fun shows_empty_state_when_nothing_saved() = runTest {
        viewModel = SavedViewModel(fakeSaved)

        var currentState: SavedUiState? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { currentState = it }
        }

        assertTrue(currentState is SavedUiState.Empty)
    }

    @Test
    fun shows_items_and_keeps_pending_flag() = runTest {
        val space = SavedSpace(1, "Café", -25.0, 28.0, "Addr", "08-17", listOf("Quiet"), 1000L, isPendingSync = true)
        fakeSaved.savedList.value = listOf(space)

        viewModel = SavedViewModel(fakeSaved)

        var currentState: SavedUiState? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { currentState = it }
        }

        assertTrue(currentState is SavedUiState.Content)
        val content = currentState as SavedUiState.Content
        assertEquals(1, content.items.size)
        assertTrue(content.items[0].isPendingSync)
    }

    @Test
    fun pending_count_flows_into_content_state() = runTest {
        val space = SavedSpace(1, "Café", -25.0, 28.0, "Addr", "08-17", listOf("Quiet"), 1000L, isPendingSync = false)
        fakeSaved.savedList.value = listOf(space)
        fakeSaved.pendingCountFlow.value = 3

        viewModel = SavedViewModel(fakeSaved)

        var currentState: SavedUiState? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { currentState = it }
        }

        assertTrue(currentState is SavedUiState.Content)
        assertEquals(3, (currentState as SavedUiState.Content).pendingCount)
    }

    @Test
    fun remove_calls_repository_and_emits_removed_event() = runTest {
        val space = SavedSpace(1, "Café", -25.0, 28.0, "Addr", "08-17", listOf("Quiet"), 1000L, isPendingSync = false)
        fakeSaved.savedList.value = listOf(space)

        viewModel = SavedViewModel(fakeSaved)

        var emittedEvent: SavedEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { emittedEvent = it }
        }

        viewModel.remove(1)

        assertEquals(listOf(1), fakeSaved.removeCallLog)
        assertEquals(SavedEvent.RemovedMessage, emittedEvent)
    }

    @Test
    fun list_updates_when_repository_changes() = runTest {
        viewModel = SavedViewModel(fakeSaved)

        var currentState: SavedUiState? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { currentState = it }
        }

        assertTrue(currentState is SavedUiState.Empty)

        val space = SavedSpace(1, "Café", -25.0, 28.0, "Addr", "08-17", listOf("Quiet"), 1000L, isPendingSync = false)
        fakeSaved.savedList.value = listOf(space)

        assertTrue(currentState is SavedUiState.Content)
        assertEquals(1, (currentState as SavedUiState.Content).items.size)
    }
}
