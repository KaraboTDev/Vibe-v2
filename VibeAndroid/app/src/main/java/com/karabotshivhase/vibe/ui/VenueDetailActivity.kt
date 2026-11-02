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
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.databinding.ActivityVenueDetailBinding
import com.karabotshivhase.vibe.ui.detail.DetailUiState
import com.karabotshivhase.vibe.ui.detail.DetailViewModel
import com.karabotshivhase.vibe.util.HoursFormatter
import com.karabotshivhase.vibe.util.moodLabelRes
import kotlinx.coroutines.launch
import java.net.URLEncoder

class VenueDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVenueDetailBinding

    private val viewModel: DetailViewModel by viewModels {
        val venueId = intent.getIntExtra(Extras.EXTRA_VENUE_ID, -1)
        DetailViewModel.Factory((application as VibeApp).venueRepository, venueId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityVenueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.contentScrollView.clipToPadding = false

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScrollView) { view, insets ->
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

        binding.retryButton.setOnClickListener {
            viewModel.retry()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetailUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.contentScrollView.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                        }
                        is DetailUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.contentScrollView.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
                            bindDetail(state.detail)
                        }
                        is DetailUiState.NotFound -> {
                            binding.progressBar.visibility = View.GONE
                            binding.contentScrollView.visibility = View.GONE
                            binding.errorLayout.visibility = View.VISIBLE
                            binding.errorTextView.text = getString(R.string.venue_not_found)
                            binding.retryButton.visibility = View.GONE
                        }
                        is DetailUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.contentScrollView.visibility = View.GONE
                            binding.errorLayout.visibility = View.VISIBLE
                            binding.errorTextView.text = getString(R.string.venue_load_error)
                            binding.retryButton.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun bindDetail(detail: com.karabotshivhase.vibe.venue.VenueDetail) {
        supportActionBar?.title = detail.name
        binding.nameTextView.text = detail.name

        val address = detail.address
        binding.addressTextView.text = if (address.isNullOrBlank()) {
            getString(R.string.address_not_listed)
        } else {
            address
        }

        val hoursClean = HoursFormatter.clean(detail.openingHours)
        binding.hoursTextView.text = hoursClean ?: getString(R.string.hours_not_listed)

        binding.chipGroupTags.removeAllViews()
        for (tagVote in detail.tags) {
            val chip = Chip(this).apply {
                text = moodLabelRes(tagVote.name)?.let { getString(it) } ?: tagVote.name
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.white)
                setTextColor(getColor(R.color.vibe_green))
                chipStrokeColor = getColorStateList(R.color.vibe_green)
                chipStrokeWidth = 1f
            }
            binding.chipGroupTags.addView(chip)
        }

        binding.openInMapsButton.setOnClickListener {
            try {
                val encodedName = URLEncoder.encode(detail.name, "UTF-8")
                val uri = Uri.parse("geo:${detail.latitude},${detail.longitude}?q=${detail.latitude},${detail.longitude}($encodedName)")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Snackbar.make(binding.root, R.string.maps_not_available, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
