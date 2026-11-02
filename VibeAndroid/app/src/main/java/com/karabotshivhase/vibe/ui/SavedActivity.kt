package com.karabotshivhase.vibe.ui

import android.content.Intent
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
import com.karabotshivhase.vibe.databinding.ActivitySavedBinding
import com.karabotshivhase.vibe.ui.saved.SavedAdapter
import com.karabotshivhase.vibe.ui.saved.SavedEvent
import com.karabotshivhase.vibe.ui.saved.SavedUiState
import com.karabotshivhase.vibe.ui.saved.SavedViewModel
import kotlinx.coroutines.launch

class SavedActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedBinding
    private lateinit var adapter: SavedAdapter

    private val viewModel: SavedViewModel by viewModels {
        SavedViewModel.Factory((application as VibeApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySavedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom,
            )
            insets
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

                                adapter.submitList(state.items)
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

    override fun onStart() {
        super.onStart()
        (application as VibeApp).scheduler.requestSync()
    }
}
