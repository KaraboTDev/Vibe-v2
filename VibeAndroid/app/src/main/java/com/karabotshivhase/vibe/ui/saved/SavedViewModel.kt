package com.karabotshivhase.vibe.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.data.favorites.SavedSpace
import com.karabotshivhase.vibe.data.favorites.SavedSpaces
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SavedUiState {
    object Loading : SavedUiState
    object Empty : SavedUiState
    data class Content(val items: List<SavedSpace>, val pendingCount: Int) : SavedUiState
}

sealed interface SavedEvent {
    object RemovedMessage : SavedEvent
}

class SavedViewModel(
    private val saved: SavedSpaces,
) : ViewModel() {

    private val _events = Channel<SavedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<SavedUiState> = combine(
        saved.observeSaved(),
        saved.observePendingCount(),
    ) { items, pendingCount ->
        if (items.isEmpty()) {
            SavedUiState.Empty
        } else {
            SavedUiState.Content(items, pendingCount)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SavedUiState.Loading,
    )

    fun remove(venueId: Int) {
        viewModelScope.launch {
            saved.remove(venueId)
            _events.send(SavedEvent.RemovedMessage)
        }
    }

    companion object {
        fun Factory(saved: SavedSpaces) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SavedViewModel(saved) as T
            }
        }
    }
}
