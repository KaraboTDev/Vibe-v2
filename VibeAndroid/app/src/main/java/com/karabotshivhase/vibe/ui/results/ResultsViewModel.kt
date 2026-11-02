package com.karabotshivhase.vibe.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.venue.LoadFailure
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.Venue
import com.karabotshivhase.vibe.venue.VenueSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ResultsUiState {
    object Loading : ResultsUiState
    data class Success(val venues: List<Venue>) : ResultsUiState
    object Empty : ResultsUiState
    data class Error(val reason: LoadFailure) : ResultsUiState
}

class ResultsViewModel(
    private val venueSource: VenueSource,
    val mood: String,
    val areaName: String,
    val lat: Double,
    val lng: Double,
    val radiusKm: Double = 10.0,
    val limit: Int = 20,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadVenues()
    }

    fun loadVenues() {
        _uiState.update { ResultsUiState.Loading }
        viewModelScope.launch {
            when (val result = venueSource.search(mood, lat, lng, radiusKm, limit)) {
                is LoadResult.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.update { ResultsUiState.Empty }
                    } else {
                        _uiState.update { ResultsUiState.Success(result.data) }
                    }
                }
                is LoadResult.Failure -> {
                    _uiState.update { ResultsUiState.Error(result.reason) }
                }
            }
        }
    }

    fun retry() {
        loadVenues()
    }

    companion object {
        fun Factory(
            venueSource: VenueSource,
            mood: String,
            areaName: String,
            lat: Double,
            lng: Double,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ResultsViewModel(venueSource, mood, areaName, lat, lng) as T
            }
        }
    }
}
