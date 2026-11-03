package com.karabotshivhase.vibe.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.rootScrollView.clipToPadding = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0,
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                maxOf(systemBars.bottom, ime.bottom),
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

        // Logout Button
        binding.logoutButton.setOnClickListener {
            showLogoutDialog()
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

                        binding.emailTextView.text = getString(R.string.home_signed_in_as, state.email.orEmpty())

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
