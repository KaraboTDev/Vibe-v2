package com.karabotshivhase.vibe

import com.karabotshivhase.vibe.data.settings.UserSettings
import com.karabotshivhase.vibe.notification.VibeFirebaseMessagingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTest {

    @Test
    fun constants_and_channel_id_are_defined() {
        assertEquals("vibe_notifications", VibeFirebaseMessagingService.CHANNEL_ID)
        assertEquals(1001, VibeFirebaseMessagingService.NOTIFICATION_ID)
    }

    @Test
    fun default_settings_have_notifications_enabled() {
        val settings = UserSettings()
        assertTrue(settings.notificationsEnabled)
    }
}
