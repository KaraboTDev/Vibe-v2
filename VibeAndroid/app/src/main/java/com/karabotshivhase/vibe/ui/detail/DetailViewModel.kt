package com.karabotshivhase.vibe.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.venue.LoadFailure
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.VenueDetail
import com.karabotshivhase.vibe.venue.VenueSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val detail: VenueDetail) : DetailUiState
    object NotFound : DetailUiState
    object Error : DetailUiState
}

class DetailViewModel(
    private val venueSource: VenueSource,
    val venueId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadVenue()
    }

    fun loadVenue() {
        _uiState.update { DetailUiState.Loading }
        viewModelScope.launch {
            when (val result = venueSource.getVenue(venueId)) {
                is LoadResult.Success -> {
                    _uiState.update { DetailUiState.Success(result.data) }
                }
                is LoadResult.Failure -> {
                    if (result.reason == LoadFailure.NotFound) {
                        _uiState.update { DetailUiState.NotFound }
                    } else {
                        _uiState.update { DetailUiState.Error }
                    }
                }
            }
        }
    }

    fun retry() {
        loadVenue()
    }

    companion object {
        fun Factory(
            venueSource: VenueSource,
            venueId: Int,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(venueSource, venueId) as T
            }
        }
    }
}
