package com.karabotshivhase.vibe.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.karabotshivhase.vibe.HomeActivity
import com.karabotshivhase.vibe.LoginActivity
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.databinding.ActivitySettingsBinding
import com.karabotshivhase.vibe.ui.settings.SettingsEvent
import com.karabotshivhase.vibe.ui.settings.SettingsViewModel
import com.karabotshivhase.vibe.util.AppLocale
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: SettingsViewModel by viewModels {
        val app = application as VibeApp
        val email = app.tokenStore.email()
        SettingsViewModel.Factory(app.settingsRepository, app.tokenStore, email)
    }

    private val languageCodes = listOf("en", "zu", "af")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.rootScrollView.clipToPadding = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0,
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom,
            )
            insets
        }

        // Language Dropdown
        val languageLabels = listOf(
            getString(R.string.language_english),
            getString(R.string.language_zulu),
            getString(R.string.language_afrikaans),
        )
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languageLabels)
        binding.autoCompleteLanguage.setAdapter(langAdapter)
        binding.autoCompleteLanguage.setOnItemClickListener { _, _, position, _ ->
            val code = languageCodes[position]
            viewModel.selectLanguage(code)
        }

        // Area Dropdown
        val areaNames = AreaPresets.all.map { it.name }
        val areaAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, areaNames)
        binding.autoCompleteArea.setAdapter(areaAdapter)
        binding.autoCompleteArea.setOnItemClickListener { _, _, position, _ ->
            val selected = AreaPresets.all[position]
            viewModel.selectArea(selected.name)
        }

        // Notifications Toggle Row
        binding.notificationsRow.setOnClickListener {
            val nextState = !binding.switchNotifications.isChecked
            viewModel.setNotifications(nextState)
        }

        // Long-click Notifications Row to trigger an instant test notification for proof screenshots!
        binding.notificationsRow.setOnLongClickListener {
            triggerTestNotification()
            true
        }

        // Logout Button
        binding.logoutButton.setOnClickListener {
            showLogoutDialog()
        }

        binding.bottomNavigationView.selectedItemId = R.id.nav_profile
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> {
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_saved -> {
                    startActivity(Intent(this, SavedActivity::class.java))
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressBar.visibility = if (state.isBusy) View.VISIBLE else View.GONE
                        binding.autoCompleteLanguage.isEnabled = !state.isBusy
                        binding.autoCompleteArea.isEnabled = !state.isBusy
                        binding.notificationsRow.isEnabled = !state.isBusy
                        binding.switchNotifications.isEnabled = !state.isBusy
                        binding.logoutButton.isEnabled = !state.isBusy

                        val langIndex = languageCodes.indexOf(state.language).coerceAtLeast(0)
                        binding.autoCompleteLanguage.setText(languageLabels[langIndex], false)

                        binding.autoCompleteArea.setText(state.areaName, false)

                        binding.switchNotifications.isChecked = state.notificationsEnabled

                        val userEmail = state.email.orEmpty()
                        val displayName = userEmail.substringBefore("@").ifBlank { "User" }
                        binding.profileNameTextView.text = displayName
                        binding.emailTextView.text = getString(R.string.home_signed_in_as, userEmail)

                        state.messageRes?.let { msgRes ->
                            Snackbar.make(binding.root, msgRes, Snackbar.LENGTH_LONG).show()
                            viewModel.messageShown()
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SettingsEvent.ApplyLanguage -> {
                                AppLocale.apply(event.code)
                            }
                            is SettingsEvent.SessionExpired -> {
                                Snackbar.make(binding.root, R.string.session_expired, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.login_action) {
                                        viewModel.logout()
                                    }
                                    .show()
                            }
                            is SettingsEvent.NavigateLogin -> {
                                val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "vibe_notifications",
            getString(R.string.settings_notifications),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.settings_notifications_summary)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, VenueDetailActivity::class.java).apply {
            putExtra(Extras.EXTRA_VENUE_ID, 1)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, "vibe_notifications")
            .setSmallIcon(R.drawable.ic_vibe_logo_small)
            .setContentTitle("New Quiet Spot Found!")
            .setContentText("Tap to check out Lumina Roasters nearby.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(1001, notification)
            Snackbar.make(binding.root, "Test notification triggered!", Snackbar.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Snackbar.make(binding.root, "Please grant notification permissions in system settings.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout_button) { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
