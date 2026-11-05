package com.karabotshivhase.vibe.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.databinding.ActivityResultsBinding
import com.karabotshivhase.vibe.ui.results.ResultsUiState
import com.karabotshivhase.vibe.ui.results.ResultsViewModel
import com.karabotshivhase.vibe.ui.results.VenuesAdapter
import com.karabotshivhase.vibe.util.moodLabelRes
import kotlinx.coroutines.launch

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding
    private lateinit var adapter: VenuesAdapter

    private val viewModel: ResultsViewModel by viewModels {
        val mood = intent.getStringExtra(Extras.EXTRA_MOOD).orEmpty()
        val areaName = intent.getStringExtra(Extras.EXTRA_AREA_NAME).orEmpty()
        val lat = intent.getDoubleExtra(Extras.EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(Extras.EXTRA_LNG, 0.0)
        ResultsViewModel.Factory((application as VibeApp).venueRepository, mood, areaName, lat, lng)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val displayMood = moodLabelRes(viewModel.mood)?.let { getString(it) } ?: viewModel.mood
        binding.moodTitleTextView.text = displayMood

        populateSubTagFilterChips(viewModel.mood)

        adapter = VenuesAdapter(viewModel.mood) { venue ->
            val intent = Intent(this, VenueDetailActivity::class.java).apply {
                putExtra(Extras.EXTRA_VENUE_ID, venue.id)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.clipToPadding = false

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

        binding.mapViewButton.setOnClickListener {
            openMapsArea(viewModel.lat, viewModel.lng, viewModel.areaName)
        }

        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }

        binding.bottomNavigationView.selectedItemId = R.id.nav_discover
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> {
                    finish()
                    true
                }
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ResultsUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyLayout.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                            binding.venueCountTextView.text = getString(R.string.searching_spaces)
                        }
                        is ResultsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.emptyLayout.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                            val count = state.venues.size
                            binding.venueCountTextView.text = getString(R.string.venues_found_count, count)
                            adapter.submitList(state.venues)
                        }
                        is ResultsUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyLayout.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
                            binding.venueCountTextView.text = getString(R.string.venues_found_count, 0)
                            binding.emptyTextView.text = getString(
                                R.string.results_empty,
                                viewModel.mood,
                                viewModel.areaName,
                            )
                        }
                        is ResultsUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyLayout.visibility = View.GONE
                            binding.errorLayout.visibility = View.VISIBLE
                            binding.venueCountTextView.text = getString(R.string.couldnt_load_venues)
                        }
                    }
                }
            }
        }
    }

    private fun populateSubTagFilterChips(mood: String) {
        binding.subTagsContainer.removeAllViews()
        val normalized = mood.trim().lowercase()

        val chips = when {
            normalized.contains("quiet") -> listOf(
                getString(R.string.chip_no_loud_music),
                getString(R.string.chip_excellent_wifi),
                getString(R.string.chip_comfy_seating),
            )
            normalized.contains("social") -> listOf("Good Energy", "Lively Music", "Group Seating", "Craft Drinks")
            normalized.contains("focus") -> listOf("Deep Work", "Power Outlets", "Ergonomic Chairs", "Silent Corner")
            normalized.contains("creative") -> listOf("Inspiring Art", "Natural Light", "Outdoor Terrace", "Design Vibe")
            normalized.contains("wifi") || normalized.contains("cheap") -> listOf(
                getString(R.string.chip_excellent_wifi),
                "Power Outlets",
                "Unlimited Data",
                "Laptop Friendly"
            )
            else -> listOf(
                getString(R.string.chip_no_loud_music),
                getString(R.string.chip_excellent_wifi),
                getString(R.string.chip_comfy_seating)
            )
        }

        for ((index, chipText) in chips.withIndex()) {
            val textView = TextView(this).apply {
                text = chipText
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@ResultsActivity, R.color.vibe_green))
                background = ContextCompat.getDrawable(this@ResultsActivity, R.drawable.bg_chip_light)
                val padH = (14 * resources.displayMetrics.density).toInt()
                val padV = (8 * resources.displayMetrics.density).toInt()
                setPadding(padH, padV, padH, padV)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (index > 0) {
                    marginStart = (8 * resources.displayMetrics.density).toInt()
                }
            }
            binding.subTagsContainer.addView(textView, params)
        }
    }

    private fun openMapsArea(latitude: Double, longitude: Double, areaName: String) {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($areaName)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.maps_not_available, Snackbar.LENGTH_LONG).show()
        }
    }
}
