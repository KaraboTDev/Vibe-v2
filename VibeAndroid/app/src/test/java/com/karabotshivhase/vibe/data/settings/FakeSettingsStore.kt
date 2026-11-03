package com.karabotshivhase.vibe.data.settings

class FakeSettingsStore : SettingsStore {
    var settings = UserSettings()
    var dirtyFlag = false

    override fun read(): UserSettings = settings

    override fun write(s: UserSettings) {
        settings = s
    }

    override fun isDirty(): Boolean = dirtyFlag

    override fun setDirty(value: Boolean) {
        dirtyFlag = value
    }

    override fun clear() {
        settings = UserSettings()
        dirtyFlag = false
    }
}
