package com.karabotshivhase.vibe.ui.home

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.venue.FakeVenueSource
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.Tag
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSource: FakeVenueSource
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeSource = FakeVenueSource()
    }

    @Test
    fun loads_tags_on_init() = runTest {
        fakeSource.tagsResult = LoadResult.Success(listOf(Tag(1, "Quiet")))
        viewModel = HomeViewModel(fakeSource)

        assertEquals(1, fakeSource.tagsCallCount)
        assertTrue(viewModel.tagsState.value is TagsUiState.Success)
    }

    @Test
    fun tag_load_failure_sets_error_and_retry_reloads() = runTest {
        fakeSource.tagsResult = LoadResult.Failure(com.karabotshivhase.vibe.venue.LoadFailure.Network)
        viewModel = HomeViewModel(fakeSource)

        assertTrue(viewModel.tagsState.value is TagsUiState.Error)

        fakeSource.tagsResult = LoadResult.Success(listOf(Tag(1, "Quiet")))
        viewModel.retryTags()

        assertEquals(2, fakeSource.tagsCallCount)
        assertTrue(viewModel.tagsState.value is TagsUiState.Success)
    }

    @Test
    fun search_not_enabled_until_mood_selected() = runTest {
        viewModel = HomeViewModel(fakeSource)

        assertFalse(viewModel.canSearch.value)

        viewModel.selectMood("Quiet")
        assertTrue(viewModel.canSearch.value)

        viewModel.selectMood(null)
        assertFalse(viewModel.canSearch.value)
    }

    @Test
    fun selecting_area_updates_state() = runTest {
        viewModel = HomeViewModel(fakeSource)

        assertEquals(AreaPresets.default, viewModel.selectedArea.value)

        viewModel.selectArea(AreaPresets.Brooklyn)
        assertEquals(AreaPresets.Brooklyn, viewModel.selectedArea.value)
    }
}
