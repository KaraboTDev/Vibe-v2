package com.karabotshivhase.vibe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.Tag
import com.karabotshivhase.vibe.venue.VenueSource
import com.karabotshivhase.vibe.util.Area
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TagsUiState {
    object Loading : TagsUiState
    data class Success(val tags: List<Tag>) : TagsUiState
    object Error : TagsUiState
}

class HomeViewModel(
    private val venueSource: VenueSource,
    val initialArea: Area = AreaPresets.default,
) : ViewModel() {

    private var lastDefaultAreaName = initialArea.name

    private val _tagsState = MutableStateFlow<TagsUiState>(TagsUiState.Loading)
    val tagsState: StateFlow<TagsUiState> = _tagsState.asStateFlow()

    private val _selectedMood = MutableStateFlow<String?>(null)
    val selectedMood: StateFlow<String?> = _selectedMood.asStateFlow()

    private val _selectedArea = MutableStateFlow<Area>(initialArea)
    val selectedArea: StateFlow<Area> = _selectedArea.asStateFlow()

    private val _canSearch = MutableStateFlow(false)
    val canSearch: StateFlow<Boolean> = _canSearch.asStateFlow()

    init {
        loadTags()
    }

    fun loadTags() {
        _tagsState.update { TagsUiState.Loading }
        viewModelScope.launch {
            when (val result = venueSource.getTags()) {
                is LoadResult.Success -> {
                    _tagsState.update { TagsUiState.Success(result.data) }
                }
                is LoadResult.Failure -> {
                    _tagsState.update { TagsUiState.Error }
                }
            }
        }
    }

    fun retryTags() {
        loadTags()
    }

    fun selectMood(name: String?) {
        _selectedMood.update { name }
        _canSearch.update { !name.isNullOrBlank() }
    }

    fun selectArea(area: Area) {
        _selectedArea.update { area }
    }

    fun onDefaultAreaChanged(area: Area) {
        if (area.name != lastDefaultAreaName) {
            lastDefaultAreaName = area.name
            _selectedArea.update { area }
        }
    }

    companion object {
        fun Factory(
            venueSource: VenueSource,
            initialArea: Area = AreaPresets.default,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(venueSource, initialArea) as T
            }
        }
    }
}
