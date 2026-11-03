package com.karabotshivhase.vibe

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
import com.google.android.material.chip.Chip
import com.karabotshivhase.vibe.data.settings.RefreshResult
import com.karabotshivhase.vibe.databinding.ActivityHomeBinding
import com.karabotshivhase.vibe.ui.Extras
import com.karabotshivhase.vibe.ui.ResultsActivity
import com.karabotshivhase.vibe.ui.SavedActivity
import com.karabotshivhase.vibe.ui.SettingsActivity
import com.karabotshivhase.vibe.ui.home.HomeViewModel
import com.karabotshivhase.vibe.ui.home.TagsUiState
import com.karabotshivhase.vibe.util.AppLocale
import com.karabotshivhase.vibe.util.AreaPresets
import com.karabotshivhase.vibe.util.moodLabelRes
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

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_saved_spaces -> {
                    startActivity(Intent(this, SavedActivity::class.java))
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
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

        // Setup Area Dropdown
        val areaNames = AreaPresets.all.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, areaNames)
        binding.autoCompleteArea.setAdapter(adapter)
        binding.autoCompleteArea.setText(viewModel.selectedArea.value.name, false)
        binding.autoCompleteArea.setOnItemClickListener { _, _, position, _ ->
            val selected = AreaPresets.all[position]
            viewModel.selectArea(selected)
        }

        // ChipGroup selection listener
        binding.chipGroupMoods.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                viewModel.selectMood(null)
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                val moodName = chip?.tag as? String
                viewModel.selectMood(moodName)
            }
        }

        binding.tagsRetryButton.setOnClickListener {
            viewModel.retryTags()
        }

        binding.findSpacesButton.setOnClickListener {
            val mood = viewModel.selectedMood.value
            val area = viewModel.selectedArea.value
            if (!mood.isNullOrBlank()) {
                val intent = Intent(this, ResultsActivity::class.java).apply {
                    putExtra(Extras.EXTRA_MOOD, mood)
                    putExtra(Extras.EXTRA_AREA_NAME, area.name)
                    putExtra(Extras.EXTRA_LAT, area.latitude)
                    putExtra(Extras.EXTRA_LNG, area.longitude)
                }
                startActivity(intent)
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tagsState.collect { state ->
                        when (state) {
                            is TagsUiState.Loading -> {
                                binding.tagsProgressBar.visibility = View.VISIBLE
                                binding.tagsErrorLayout.visibility = View.GONE
                                binding.chipGroupMoods.visibility = View.GONE
                            }
                            is TagsUiState.Success -> {
                                binding.tagsProgressBar.visibility = View.GONE
                                binding.tagsErrorLayout.visibility = View.GONE
                                binding.chipGroupMoods.visibility = View.VISIBLE
                                populateMoodChips(state.tags)
                            }
                            is TagsUiState.Error -> {
                                binding.tagsProgressBar.visibility = View.GONE
                                binding.tagsErrorLayout.visibility = View.VISIBLE
                                binding.chipGroupMoods.visibility = View.GONE
                            }
                        }
                    }
                }
                launch {
                    viewModel.selectedArea.collect { selectedArea ->
                        binding.autoCompleteArea.setText(selectedArea.name, false)
                    }
                }
                launch {
                    viewModel.canSearch.collect { canSearch ->
                        binding.findSpacesButton.isEnabled = canSearch
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as VibeApp
        val defaultArea = AreaPresets.byName(app.settingsRepository.current().defaultAreaName)
        viewModel.onDefaultAreaChanged(defaultArea)
    }

    private fun populateMoodChips(tags: List<com.karabotshivhase.vibe.venue.Tag>) {
        binding.chipGroupMoods.removeAllViews()
        for (tagItem in tags) {
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = moodLabelRes(tagItem.name)?.let { getString(it) } ?: tagItem.name
                setTag(tagItem.name)
                isCheckable = true
                isClickable = true
                setChipBackgroundColorResource(R.color.white)
                setTextColor(getColor(R.color.vibe_green))
                chipStrokeColor = getColorStateList(R.color.vibe_green)
                chipStrokeWidth = 1f

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        setChipBackgroundColorResource(R.color.vibe_green)
                        setTextColor(getColor(R.color.white))
                    } else {
                        setChipBackgroundColorResource(R.color.white)
                        setTextColor(getColor(R.color.vibe_green))
                    }
                }
            }
            binding.chipGroupMoods.addView(chip)
        }
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
