package com.karabotshivhase.vibe.ui.detail

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.data.favorites.FakeSavedSpaces
import com.karabotshivhase.vibe.data.favorites.SavedSpace
import com.karabotshivhase.vibe.venue.FakeVenueSource
import com.karabotshivhase.vibe.venue.LoadFailure
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.TagVote
import com.karabotshivhase.vibe.venue.VenueDetail
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
class DetailViewModelSavedTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSource: FakeVenueSource
    private lateinit var fakeSaved: FakeSavedSpaces
    private lateinit var viewModel: DetailViewModel

    private val sampleDetail = VenueDetail(1, "Coffee Shop", -25.0, 28.0, "123 St", "08-17", listOf(TagVote("Quiet", 1)))

    @Before
    fun setUp() {
        fakeSource = FakeVenueSource()
        fakeSaved = FakeSavedSpaces()
    }

    @Test
    fun isSaved_reflects_repository_flow() = runTest {
        fakeSource.venueResult = LoadResult.Success(sampleDetail)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        var isSaved = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isSaved.collect { isSaved = it }
        }

        assertFalse(isSaved)

        fakeSaved.save(sampleDetail)
        assertTrue(isSaved)
    }

    @Test
    fun toggleSave_when_not_saved_saves_the_loaded_detail() = runTest {
        fakeSource.venueResult = LoadResult.Success(sampleDetail)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        var isSaved = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isSaved.collect { isSaved = it }
        }

        viewModel.toggleSave()

        assertEquals(1, fakeSaved.saveCallLog.size)
        assertEquals(sampleDetail, fakeSaved.saveCallLog[0])
        assertTrue(isSaved)
    }

    @Test
    fun toggleSave_when_saved_removes() = runTest {
        fakeSource.venueResult = LoadResult.Success(sampleDetail)
        fakeSaved.save(sampleDetail)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        var isSaved = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isSaved.collect { isSaved = it }
        }

        assertTrue(isSaved)

        viewModel.toggleSave()

        assertEquals(listOf(1), fakeSaved.removeCallLog)
        assertFalse(isSaved)
    }

    @Test
    fun toggleSave_before_detail_loaded_does_nothing() = runTest {
        fakeSource.venueResult = LoadResult.Failure(LoadFailure.Network)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        viewModel.toggleSave()

        assertEquals(0, fakeSaved.saveCallLog.size)
        assertEquals(0, fakeSaved.removeCallLog.size)
    }

    @Test
    fun network_error_with_saved_copy_shows_OfflineCopy() = runTest {
        val savedSpace = SavedSpace(1, "Offline Space", -25.0, 28.0, "123 St", "08-17", listOf("Quiet"), 1000L, false)
        fakeSaved.savedList.value = listOf(savedSpace)

        fakeSource.venueResult = LoadResult.Failure(LoadFailure.Network)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        assertTrue(viewModel.uiState.value is DetailUiState.OfflineCopy)
        val copy = (viewModel.uiState.value as DetailUiState.OfflineCopy).space
        assertEquals("Offline Space", copy.name)
    }

    @Test
    fun network_error_without_saved_copy_shows_error() = runTest {
        fakeSource.venueResult = LoadResult.Failure(LoadFailure.Network)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        assertTrue(viewModel.uiState.value is DetailUiState.Error)
    }

    @Test
    fun not_found_does_not_fall_back_to_saved_copy() = runTest {
        val savedSpace = SavedSpace(1, "Saved Space", -25.0, 28.0, "123 St", "08-17", listOf("Quiet"), 1000L, false)
        fakeSaved.savedList.value = listOf(savedSpace)

        fakeSource.venueResult = LoadResult.Failure(LoadFailure.NotFound)
        viewModel = DetailViewModel(fakeSource, 1, fakeSaved)

        assertTrue(viewModel.uiState.value is DetailUiState.NotFound)
    }
}
