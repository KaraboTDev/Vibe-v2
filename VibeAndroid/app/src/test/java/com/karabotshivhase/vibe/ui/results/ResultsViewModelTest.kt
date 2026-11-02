package com.karabotshivhase.vibe.ui.results

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.venue.FakeVenueSource
import com.karabotshivhase.vibe.venue.LoadFailure
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.Venue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSource: FakeVenueSource
    private lateinit var viewModel: ResultsViewModel

    @Before
    fun setUp() {
        fakeSource = FakeVenueSource()
    }

    @Test
    fun success_shows_items() = runTest {
        val venues = listOf(Venue(1, "Café", -25.0, 28.0, "Addr", "08-17", listOf("Quiet"), 200))
        fakeSource.searchResult = LoadResult.Success(venues)

        viewModel = ResultsViewModel(fakeSource, "Quiet", "Hatfield", -25.7, 28.2)

        assertTrue(viewModel.uiState.value is ResultsUiState.Success)
        assertEquals(venues, (viewModel.uiState.value as ResultsUiState.Success).venues)
    }

    @Test
    fun empty_response_shows_empty_state() = runTest {
        fakeSource.searchResult = LoadResult.Success(emptyList())

        viewModel = ResultsViewModel(fakeSource, "Quiet", "Hatfield", -25.7, 28.2)

        assertTrue(viewModel.uiState.value is ResultsUiState.Empty)
    }

    @Test
    fun failure_shows_error_then_retry_succeeds() = runTest {
        fakeSource.searchResult = LoadResult.Failure(LoadFailure.Network)
        viewModel = ResultsViewModel(fakeSource, "Quiet", "Hatfield", -25.7, 28.2)

        assertTrue(viewModel.uiState.value is ResultsUiState.Error)
        assertEquals(LoadFailure.Network, (viewModel.uiState.value as ResultsUiState.Error).reason)

        val venues = listOf(Venue(1, "Café", -25.0, 28.0, "Addr", "08-17", listOf("Quiet"), 200))
        fakeSource.searchResult = LoadResult.Success(venues)
        viewModel.retry()

        assertTrue(viewModel.uiState.value is ResultsUiState.Success)
    }

    @Test
    fun passes_expected_arguments_to_source() = runTest {
        viewModel = ResultsViewModel(fakeSource, "Quiet", "Hatfield", -25.7, 28.2, 15.0, 10)

        assertEquals("Quiet", fakeSource.lastSearchMood)
        assertEquals(-25.7, fakeSource.lastSearchLat!!, 0.0001)
        assertEquals(28.2, fakeSource.lastSearchLng!!, 0.0001)
        assertEquals(15.0, fakeSource.lastSearchRadius!!, 0.0001)
        assertEquals(10, fakeSource.lastSearchLimit)
    }
}
