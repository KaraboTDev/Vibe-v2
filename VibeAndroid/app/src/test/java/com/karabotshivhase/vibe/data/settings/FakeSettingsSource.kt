package com.karabotshivhase.vibe.data.settings

import com.karabotshivhase.vibe.util.AreaPresets

class FakeSettingsSource : SettingsSource {
    var currentSettings = UserSettings("en", AreaPresets.Hatfield.name, true)
    var updateResultToReturn: UpdateResult = UpdateResult.Synced
    var refreshResultToReturn: RefreshResult = RefreshResult.Unchanged

    val updateCallLog = mutableListOf<SettingsChange>()
    var refreshCallCount = 0
    var clearCallCount = 0

    override fun current(): UserSettings = currentSettings

    override suspend fun update(change: SettingsChange): UpdateResult {
        updateCallLog.add(change)
        if (updateResultToReturn == UpdateResult.Synced || updateResultToReturn == UpdateResult.SavedLocally) {
            when (change) {
                is SettingsChange.Language -> currentSettings = currentSettings.copy(language = LanguageCodes.normalize(change.code))
                is SettingsChange.Area -> currentSettings = currentSettings.copy(defaultAreaName = AreaPresets.byName(change.name).name)
                is SettingsChange.Notifications -> currentSettings = currentSettings.copy(notificationsEnabled = change.enabled)
            }
        }
        return updateResultToReturn
    }

    override suspend fun refresh(): RefreshResult {
        refreshCallCount++
        return refreshResultToReturn
    }

    override fun clear() {
        clearCallCount++
        currentSettings = UserSettings()
    }
}
