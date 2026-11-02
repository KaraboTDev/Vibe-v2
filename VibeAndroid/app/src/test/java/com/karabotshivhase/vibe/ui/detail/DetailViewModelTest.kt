package com.karabotshivhase.vibe.ui.detail

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.venue.FakeVenueSource
import com.karabotshivhase.vibe.venue.LoadFailure
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.VenueDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSource: FakeVenueSource
    private lateinit var viewModel: DetailViewModel

    @Before
    fun setUp() {
        fakeSource = FakeVenueSource()
    }

    @Test
    fun success_shows_detail() = runTest {
        val detail = VenueDetail(1, "Coffee Shop", -25.0, 28.0, "123 St", "08-17", emptyList())
        fakeSource.venueResult = LoadResult.Success(detail)

        viewModel = DetailViewModel(fakeSource, 1)

        assertTrue(viewModel.uiState.value is DetailUiState.Success)
        assertEquals(detail, (viewModel.uiState.value as DetailUiState.Success).detail)
        assertEquals(1, fakeSource.lastVenueId)
    }

    @Test
    fun not_found_shows_not_found_state() = runTest {
        fakeSource.venueResult = LoadResult.Failure(LoadFailure.NotFound)

        viewModel = DetailViewModel(fakeSource, 999)

        assertTrue(viewModel.uiState.value is DetailUiState.NotFound)
    }

    @Test
    fun retry_after_network_error_succeeds() = runTest {
        fakeSource.venueResult = LoadResult.Failure(LoadFailure.Network)
        viewModel = DetailViewModel(fakeSource, 1)

        assertTrue(viewModel.uiState.value is DetailUiState.Error)

        val detail = VenueDetail(1, "Coffee Shop", -25.0, 28.0, "123 St", "08-17", emptyList())
        fakeSource.venueResult = LoadResult.Success(detail)
        viewModel.retry()

        assertTrue(viewModel.uiState.value is DetailUiState.Success)
    }
}
