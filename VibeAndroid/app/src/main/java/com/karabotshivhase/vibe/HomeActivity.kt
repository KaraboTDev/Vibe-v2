package com.karabotshivhase.vibe

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.karabotshivhase.vibe.data.settings.RefreshResult
import com.karabotshivhase.vibe.databinding.ActivityHomeBinding
import com.karabotshivhase.vibe.ui.Extras
import com.karabotshivhase.vibe.ui.ResultsActivity
import com.karabotshivhase.vibe.ui.SavedActivity
import com.karabotshivhase.vibe.ui.SettingsActivity
import com.karabotshivhase.vibe.ui.home.HomeViewModel
import com.karabotshivhase.vibe.util.AppLocale
import com.karabotshivhase.vibe.util.AreaPresets
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels {
        val app = application as VibeApp
        val defaultArea = AreaPresets.byName(app.settingsRepository.current().defaultAreaName)
        HomeViewModel.Factory(app.venueRepository, defaultArea)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as VibeApp
        if (!app.tokenStore.isLoggedIn()) {
            openLogin()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.cardQuietFocus.setOnClickListener { openResults("Quiet") }
        binding.cardSocialLively.setOnClickListener { openResults("Social") }
        binding.cardFastWifi.setOnClickListener { openResults("Cheap Wifi") }
        binding.cardOutdoorEscape.setOnClickListener { openResults("Creative") }
        binding.cardHiddenGems.setOnClickListener { openResults("Focus") }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.bottomNavigationView.selectedItemId = R.id.nav_discover
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> true
                R.id.nav_saved -> {
                    startActivity(Intent(this, SavedActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }

        lifecycleScope.launch {
            when (val refreshResult = app.settingsRepository.refresh()) {
                is RefreshResult.Updated -> {
                    AppLocale.apply(refreshResult.settings.language)
                }
                else -> {}
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as VibeApp
        val defaultArea = AreaPresets.byName(app.settingsRepository.current().defaultAreaName)
        viewModel.onDefaultAreaChanged(defaultArea)
        binding.bottomNavigationView.selectedItemId = R.id.nav_discover
    }

    private fun openResults(mood: String) {
        val area = viewModel.selectedArea.value
        val intent = Intent(this, ResultsActivity::class.java).apply {
            putExtra(Extras.EXTRA_MOOD, mood)
            putExtra(Extras.EXTRA_AREA_NAME, area.name)
            putExtra(Extras.EXTRA_LAT, area.latitude)
            putExtra(Extras.EXTRA_LNG, area.longitude)
        }
        startActivity(intent)
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
