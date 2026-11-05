package com.karabotshivhase.vibe.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.karabotshivhase.vibe.HomeActivity
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.ui.Extras
import com.karabotshivhase.vibe.ui.VenueDetailActivity

class VibeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM registration token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val app = applicationContext as? VibeApp ?: return
        if (!app.settingsRepository.current().notificationsEnabled) {
            return
        }

        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        val venueIdStr = message.data["venueId"]

        showNotification(title, body, venueIdStr?.toIntOrNull())
    }

    private fun showNotification(title: String, body: String, venueId: Int?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.settings_notifications),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.settings_notifications_summary)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = if (venueId != null) {
            Intent(this, VenueDetailActivity::class.java).apply {
                putExtra(Extras.EXTRA_VENUE_ID, venueId)
            }
        } else {
            Intent(this, HomeActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vibe_logo_small)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    companion object {
        const val CHANNEL_ID = "vibe_notifications"
        const val NOTIFICATION_ID = 1001
    }
}
