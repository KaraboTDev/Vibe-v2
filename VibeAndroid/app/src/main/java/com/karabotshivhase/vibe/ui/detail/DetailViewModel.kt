package com.karabotshivhase.vibe.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.data.favorites.SavedSpace
import com.karabotshivhase.vibe.data.favorites.SavedSpaces
import com.karabotshivhase.vibe.venue.LoadFailure
import com.karabotshivhase.vibe.venue.LoadResult
import com.karabotshivhase.vibe.venue.VenueDetail
import com.karabotshivhase.vibe.venue.VenueSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val detail: VenueDetail) : DetailUiState
    data class OfflineCopy(val space: SavedSpace) : DetailUiState
    object NotFound : DetailUiState
    object Error : DetailUiState
}

sealed interface DetailEvent {
    object SavedMessage : DetailEvent
    object RemovedMessage : DetailEvent
}

class DetailViewModel(
    private val venueSource: VenueSource,
    val venueId: Int,
    private val saved: SavedSpaces? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val isSaved: StateFlow<Boolean> = saved?.observeIsSaved(venueId)
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        ?: MutableStateFlow(false)

    private var isToggling = false

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
                    } else if (result.reason == LoadFailure.Network) {
                        val savedSpace = saved?.getSaved(venueId)
                        if (savedSpace != null) {
                            _uiState.update { DetailUiState.OfflineCopy(savedSpace) }
                        } else {
                            _uiState.update { DetailUiState.Error }
                        }
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

    fun toggleSave() {
        if (isToggling) return
        val currentSavedStore = saved ?: return
        val currentState = _uiState.value

        if (currentState !is DetailUiState.Success && currentState !is DetailUiState.OfflineCopy) {
            return
        }

        isToggling = true
        viewModelScope.launch {
            try {
                if (isSaved.value) {
                    currentSavedStore.remove(venueId)
                    _events.send(DetailEvent.RemovedMessage)
                } else {
                    if (currentState is DetailUiState.Success) {
                        currentSavedStore.save(currentState.detail)
                        _events.send(DetailEvent.SavedMessage)
                    }
                }
            } finally {
                isToggling = false
            }
        }
    }

    companion object {
        fun Factory(
            venueSource: VenueSource,
            venueId: Int,
            saved: SavedSpaces? = null,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailViewModel(venueSource, venueId, saved) as T
            }
        }
    }
}
