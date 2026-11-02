package com.karabotshivhase.vibe.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.databinding.ActivityResultsBinding
import com.karabotshivhase.vibe.ui.results.ResultsUiState
import com.karabotshivhase.vibe.ui.results.ResultsViewModel
import com.karabotshivhase.vibe.ui.results.VenuesAdapter
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
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        supportActionBar?.title = getString(R.string.results_title, viewModel.mood)

        adapter = VenuesAdapter { venue ->
            val intent = Intent(this, VenueDetailActivity::class.java).apply {
                putExtra(Extras.EXTRA_VENUE_ID, venue.id)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom,
            )
            insets
        }

        binding.retryButton.setOnClickListener {
            viewModel.retry()
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
                        }
                        is ResultsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.emptyLayout.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                            adapter.submitList(state.venues)
                        }
                        is ResultsUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyLayout.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
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
                        }
                    }
                }
            }
        }
    }
}
