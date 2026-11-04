package com.karabotshivhase.vibe.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

        adapter = VenuesAdapter { venue ->
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
                            binding.venueCountTextView.text = "Searching spaces..."
                        }
                        is ResultsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.emptyLayout.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                            val count = state.venues.size
                            binding.venueCountTextView.text = "$count venues found"
                            adapter.submitList(state.venues)
                        }
                        is ResultsUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyLayout.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
                            binding.venueCountTextView.text = "0 venues found"
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
                            binding.venueCountTextView.text = "Couldn't load venues"
                        }
                    }
                }
            }
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
