package com.karabotshivhase.vibe.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.karabotshivhase.vibe.HomeActivity
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.data.favorites.SavedSpace
import com.karabotshivhase.vibe.databinding.ActivitySavedBinding
import com.karabotshivhase.vibe.ui.saved.SavedAdapter
import com.karabotshivhase.vibe.ui.saved.SavedEvent
import com.karabotshivhase.vibe.ui.saved.SavedUiState
import com.karabotshivhase.vibe.ui.saved.SavedViewModel
import com.karabotshivhase.vibe.util.moodLabelRes
import kotlinx.coroutines.launch

class SavedActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedBinding
    private lateinit var adapter: SavedAdapter

    private var allSavedItems: List<SavedSpace> = emptyList()
    private var searchQuery: String = ""
    private var selectedMoodFilter: String? = null

    private val viewModel: SavedViewModel by viewModels {
        SavedViewModel.Factory((application as VibeApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySavedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        adapter = SavedAdapter(
            onSpaceClick = { space ->
                val intent = Intent(this, VenueDetailActivity::class.java).apply {
                    putExtra(Extras.EXTRA_VENUE_ID, space.venueId)
                }
                startActivity(intent)
            },
            onRemoveClick = { space ->
                viewModel.remove(space.venueId)
            },
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.clipToPadding = false

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

        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            searchQuery = text?.toString().orEmpty().trim()
            applyFilters()
        }

        setupMoodPillListeners()

        binding.bottomNavigationView.selectedItemId = R.id.nav_saved
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
                R.id.nav_saved -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is SavedUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.contentLayout.visibility = View.GONE
                                binding.emptyLayout.visibility = View.GONE
                            }
                            is SavedUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.contentLayout.visibility = View.GONE
                                binding.emptyLayout.visibility = View.VISIBLE
                            }
                            is SavedUiState.Content -> {
                                binding.progressBar.visibility = View.GONE
                                binding.contentLayout.visibility = View.VISIBLE
                                binding.emptyLayout.visibility = View.GONE

                                if (state.pendingCount > 0) {
                                    binding.pendingBannerCard.visibility = View.VISIBLE
                                    binding.pendingBannerTextView.text = resources.getQuantityString(
                                        R.plurals.pending_changes,
                                        state.pendingCount,
                                        state.pendingCount,
                                    )
                                } else {
                                    binding.pendingBannerCard.visibility = View.GONE
                                }

                                allSavedItems = state.items
                                applyFilters()
                            }
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SavedEvent.RemovedMessage -> {
                                Snackbar.make(binding.root, R.string.removed_message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupMoodPillListeners() {
        binding.pillAllSaved.setOnClickListener { selectMoodPill(null) }
        binding.pillQuiet.setOnClickListener { selectMoodPill("Quiet") }
        binding.pillSocial.setOnClickListener { selectMoodPill("Social") }
        binding.pillFocus.setOnClickListener { selectMoodPill("Focus") }
        binding.pillCreative.setOnClickListener { selectMoodPill("Creative") }
        binding.pillCheapWifi.setOnClickListener { selectMoodPill("Cheap Wifi") }
    }

    private fun selectMoodPill(mood: String?) {
        selectedMoodFilter = mood
        updatePillStyles()
        applyFilters()
    }

    private fun updatePillStyles() {
        val orangeBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_orange)
        val lightBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_light)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val greenColor = ContextCompat.getColor(this, R.color.vibe_green)

        val pills = listOf(
            null to binding.pillAllSaved,
            "Quiet" to binding.pillQuiet,
            "Social" to binding.pillSocial,
            "Focus" to binding.pillFocus,
            "Creative" to binding.pillCreative,
            "Cheap Wifi" to binding.pillCheapWifi,
        )

        for ((pMood, pView) in pills) {
            val isSelected = (pMood == selectedMoodFilter)
            pView.background = if (isSelected) orangeBg else lightBg
            pView.setTextColor(if (isSelected) whiteColor else greenColor)
        }
    }

    private fun applyFilters() {
        var filtered = allSavedItems

        if (!selectedMoodFilter.isNullOrBlank()) {
            val mood = selectedMoodFilter!!
            filtered = filtered.filter { item ->
                item.tags.any { it.equals(mood, ignoreCase = true) }
            }
            val displayMood = moodLabelRes(mood)?.let { getString(it) } ?: mood
            binding.sectionTitleTextView.text = "Saved for $displayMood"
        } else {
            binding.sectionTitleTextView.text = "Saved spaces"
        }

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.address.orEmpty().contains(searchQuery, ignoreCase = true) ||
                        item.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }

        val count = filtered.size
        binding.savedCountTextView.text = if (count == 1) "1 place" else "$count places"
        adapter.submitList(filtered)
    }

    override fun onStart() {
        super.onStart()
        (application as VibeApp).scheduler.requestSync()
        binding.bottomNavigationView.selectedItemId = R.id.nav_saved
    }
}
