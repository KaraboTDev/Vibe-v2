package com.karabotshivhase.vibe.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.auth.SessionStore
import com.karabotshivhase.vibe.data.settings.LanguageCodes
import com.karabotshivhase.vibe.data.settings.RefreshResult
import com.karabotshivhase.vibe.data.settings.SettingsChange
import com.karabotshivhase.vibe.data.settings.SettingsSource
import com.karabotshivhase.vibe.data.settings.UpdateResult
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsSource,
    private val session: SessionStore,
    val email: String?,
) : ViewModel() {

    private val currentSettings = settings.current()
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            language = LanguageCodes.normalize(currentSettings.language),
            areaName = AreaPresets.byName(currentSettings.defaultAreaName).name,
            notificationsEnabled = currentSettings.notificationsEnabled,
            email = email,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val refreshResult = settings.refresh()) {
                is RefreshResult.Updated -> {
                    val oldLang = _uiState.value.language
                    val newLang = LanguageCodes.normalize(refreshResult.settings.language)
                    _uiState.update {
                        it.copy(
                            language = newLang,
                            areaName = AreaPresets.byName(refreshResult.settings.defaultAreaName).name,
                            notificationsEnabled = refreshResult.settings.notificationsEnabled,
                        )
                    }
                    if (oldLang != newLang) {
                        _events.send(SettingsEvent.ApplyLanguage(newLang))
                    }
                }
                else -> {
                    // Unchanged, Offline, SessionExpired, Failed, Skipped
                }
            }
        }
    }

    fun selectLanguage(code: String) {
        applyChange(SettingsChange.Language(code)) { newCode: String ->
            _uiState.update { it.copy(language = LanguageCodes.normalize(newCode)) }
            _events.send(SettingsEvent.ApplyLanguage(LanguageCodes.normalize(newCode)))
        }
    }

    fun selectArea(name: String) {
        applyChange(SettingsChange.Area(name)) { newName: String ->
            _uiState.update { it.copy(areaName = AreaPresets.byName(newName).name) }
        }
    }

    fun setNotifications(enabled: Boolean) {
        applyChange(SettingsChange.Notifications(enabled)) { newEnabled: Boolean ->
            _uiState.update { it.copy(notificationsEnabled = newEnabled) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> applyChange(change: SettingsChange, onSuccess: suspend (T) -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }

        viewModelScope.launch {
            try {
                when (val result = settings.update(change)) {
                    UpdateResult.Synced -> {
                        val paramValue = when (change) {
                            is SettingsChange.Language -> change.code as T
                            is SettingsChange.Area -> change.name as T
                            is SettingsChange.Notifications -> change.enabled as T
                        }
                        onSuccess(paramValue)
                        _uiState.update { it.copy(messageRes = R.string.settings_saved) }
                    }
                    UpdateResult.SavedLocally -> {
                        val paramValue = when (change) {
                            is SettingsChange.Language -> change.code as T
                            is SettingsChange.Area -> change.name as T
                            is SettingsChange.Notifications -> change.enabled as T
                        }
                        onSuccess(paramValue)
                        _uiState.update { it.copy(messageRes = R.string.settings_saved_offline) }
                    }
                    UpdateResult.InvalidInput -> {
                        _uiState.update { it.copy(messageRes = R.string.settings_invalid) }
                    }
                    UpdateResult.SessionExpired -> {
                        _events.send(SettingsEvent.SessionExpired)
                    }
                    UpdateResult.NotLoggedIn -> {
                        _events.send(SettingsEvent.NavigateLogin)
                    }
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(messageRes = null) }
    }

    fun logout() {
        session.clear()
        settings.clear()
        viewModelScope.launch {
            _events.send(SettingsEvent.NavigateLogin)
        }
    }

    companion object {
        fun Factory(
            settings: SettingsSource,
            session: SessionStore,
            email: String?,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settings, session, email) as T
            }
        }
    }
}
