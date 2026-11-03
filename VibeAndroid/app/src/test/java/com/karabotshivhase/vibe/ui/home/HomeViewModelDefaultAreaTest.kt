package com.karabotshivhase.vibe.ui.home

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.util.AreaPresets
import com.karabotshivhase.vibe.venue.FakeVenueSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelDefaultAreaTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSource: FakeVenueSource
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeSource = FakeVenueSource()
    }

    @Test
    fun initial_area_comes_from_constructor() = runTest {
        viewModel = HomeViewModel(fakeSource, AreaPresets.Brooklyn)

        assertEquals(AreaPresets.Brooklyn, viewModel.selectedArea.value)
    }

    @Test
    fun onDefaultAreaChanged_updates_selection_when_default_changed() = runTest {
        viewModel = HomeViewModel(fakeSource, AreaPresets.Brooklyn)

        viewModel.onDefaultAreaChanged(AreaPresets.Menlyn)

        assertEquals(AreaPresets.Menlyn, viewModel.selectedArea.value)
    }

    @Test
    fun onDefaultAreaChanged_keeps_manual_choice_when_default_is_unchanged() = runTest {
        viewModel = HomeViewModel(fakeSource, AreaPresets.Brooklyn)

        viewModel.selectArea(AreaPresets.Hatfield)
        assertEquals(AreaPresets.Hatfield, viewModel.selectedArea.value)

        viewModel.onDefaultAreaChanged(AreaPresets.Brooklyn)

        assertEquals(AreaPresets.Hatfield, viewModel.selectedArea.value)
    }
}
