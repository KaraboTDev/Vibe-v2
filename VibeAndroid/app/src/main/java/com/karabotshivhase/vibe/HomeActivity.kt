package com.karabotshivhase.vibe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.karabotshivhase.vibe.databinding.ActivityHomeBinding
import com.karabotshivhase.vibe.ui.Extras
import com.karabotshivhase.vibe.ui.ResultsActivity
import com.karabotshivhase.vibe.ui.home.HomeViewModel
import com.karabotshivhase.vibe.ui.home.TagsUiState
import com.karabotshivhase.vibe.util.AreaPresets
import com.karabotshivhase.vibe.util.moodLabelRes
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory((application as VibeApp).venueRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as VibeApp
        if (!app.tokenStore.isLoggedIn()) {
            openLogin()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom),
            )
            insets
        }

        val userEmail = app.tokenStore.email().orEmpty()
        binding.signedInAsTextView.text = getString(R.string.home_signed_in_as, userEmail)

        binding.logoutButton.setOnClickListener {
            app.tokenStore.clear()
            openLogin()
        }

        // Setup Area Dropdown
        val areaNames = AreaPresets.all.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, areaNames)
        binding.autoCompleteArea.setAdapter(adapter)
        binding.autoCompleteArea.setText(AreaPresets.default.name, false)
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
                    viewModel.canSearch.collect { canSearch ->
                        binding.findSpacesButton.isEnabled = canSearch
                    }
                }
            }
        }
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
