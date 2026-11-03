package com.karabotshivhase.vibe.data.settings

import com.karabotshivhase.vibe.data.favorites.RemoteResult
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsRepository(
    private val store: SettingsStore,
    private val remote: SettingsRemote,
    private val userIdProvider: () -> Int?,
) : SettingsSource {

    private val mutex = Mutex()

    override fun current(): UserSettings = store.read()

    override suspend fun update(change: SettingsChange): UpdateResult = mutex.withLock {
        if (userIdProvider() == null) {
            return@withLock UpdateResult.NotLoggedIn
        }

        val currentSettings = store.read()
        val newSettings = when (change) {
            is SettingsChange.Language -> {
                if (!LanguageCodes.isSupported(change.code)) {
                    return@withLock UpdateResult.InvalidInput
                }
                currentSettings.copy(language = LanguageCodes.normalize(change.code))
            }
            is SettingsChange.Area -> {
                val matchingPreset = AreaPresets.all.find { it.name.equals(change.name.trim(), ignoreCase = true) }
                if (matchingPreset == null) {
                    return@withLock UpdateResult.InvalidInput
                }
                currentSettings.copy(defaultAreaName = matchingPreset.name)
            }
            is SettingsChange.Notifications -> {
                currentSettings.copy(notificationsEnabled = change.enabled)
            }
        }

        store.write(newSettings)
        store.setDirty(true)

        val remotePayload = RemoteSettings(
            language = newSettings.language,
            defaultArea = newSettings.defaultAreaName,
            notificationsEnabled = newSettings.notificationsEnabled,
        )

        return@withLock when (remote.update(remotePayload)) {
            is RemoteResult.Success -> {
                store.setDirty(false)
                UpdateResult.Synced
            }
            is RemoteResult.Unauthorized -> {
                UpdateResult.SessionExpired
            }
            is RemoteResult.NetworkError,
            is RemoteResult.NotFound,
            is RemoteResult.OtherError -> {
                UpdateResult.SavedLocally
            }
        }
    }

    override suspend fun refresh(): RefreshResult = mutex.withLock {
        if (userIdProvider() == null) {
            return@withLock RefreshResult.Skipped
        }

        if (store.isDirty()) {
            val localSettings = store.read()
            val remotePayload = RemoteSettings(
                language = localSettings.language,
                defaultArea = localSettings.defaultAreaName,
                notificationsEnabled = localSettings.notificationsEnabled,
            )
            when (remote.update(remotePayload)) {
                is RemoteResult.Success -> {
                    store.setDirty(false)
                }
                is RemoteResult.NetworkError -> {
                    return@withLock RefreshResult.Offline
                }
                is RemoteResult.Unauthorized -> {
                    return@withLock RefreshResult.SessionExpired
                }
                is RemoteResult.NotFound,
                is RemoteResult.OtherError -> {
                    return@withLock RefreshResult.Failed
                }
            }
        }

        val localSettings = store.read()
        return@withLock when (val fetchResult = remote.fetch()) {
            is RemoteResult.Success -> {
                val serverSettings = fetchResult.value
                val serverLang = LanguageCodes.normalize(serverSettings.language)
                val mergedArea = serverSettings.defaultArea ?: localSettings.defaultAreaName
                val serverNotifications = serverSettings.notificationsEnabled

                if (serverLang != localSettings.language ||
                    mergedArea != localSettings.defaultAreaName ||
                    serverNotifications != localSettings.notificationsEnabled
                ) {
                    val updatedSettings = UserSettings(
                        language = serverLang,
                        defaultAreaName = mergedArea,
                        notificationsEnabled = serverNotifications,
                    )
                    store.write(updatedSettings)
                    RefreshResult.Updated(updatedSettings)
                } else {
                    RefreshResult.Unchanged
                }
            }
            is RemoteResult.NetworkError -> {
                RefreshResult.Offline
            }
            is RemoteResult.Unauthorized -> {
                RefreshResult.SessionExpired
            }
            is RemoteResult.NotFound,
            is RemoteResult.OtherError -> {
                RefreshResult.Failed
            }
        }
    }

    override fun clear() {
        store.clear()
    }
}
