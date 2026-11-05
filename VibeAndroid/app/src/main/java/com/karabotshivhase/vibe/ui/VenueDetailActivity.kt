package com.karabotshivhase.vibe.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.databinding.ActivityVenueDetailBinding
import com.karabotshivhase.vibe.ui.detail.DetailEvent
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
        val app = application as VibeApp
        DetailViewModel.Factory(app.venueRepository, venueId, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityVenueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.contentScrollView.clipToPadding = false

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

        binding.saveButton.setOnClickListener {
            viewModel.toggleSave()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
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
                                binding.offlineNoticeCard.visibility = View.GONE
                                binding.errorLayout.visibility = View.GONE
                                bindDetail(state.detail)
                            }
                            is DetailUiState.OfflineCopy -> {
                                binding.progressBar.visibility = View.GONE
                                binding.contentScrollView.visibility = View.VISIBLE
                                binding.offlineNoticeCard.visibility = View.VISIBLE
                                binding.errorLayout.visibility = View.GONE
                                bindOfflineCopy(state.space)
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

                launch {
                    viewModel.isSaved.collect { saved ->
                        updateSaveButton(saved)
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is DetailEvent.SavedMessage -> {
                                Snackbar.make(binding.root, R.string.saved_message, Snackbar.LENGTH_LONG).show()
                            }
                            is DetailEvent.RemovedMessage -> {
                                Snackbar.make(binding.root, R.string.removed_message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateSaveButton(saved: Boolean) {
        if (saved) {
            binding.saveButton.text = getString(R.string.saved_button)
            binding.saveButton.setIconResource(R.drawable.ic_bookmark_filled)
            binding.saveButton.setIconTintResource(R.color.vibe_green)
            binding.saveButton.setBackgroundColor(getColor(R.color.white))
            binding.saveButton.setTextColor(getColor(R.color.vibe_green))
            binding.saveButton.strokeColor = ContextCompat.getColorStateList(this, R.color.vibe_green)
            binding.saveButton.strokeWidth = (1 * resources.displayMetrics.density).toInt()
        } else {
            binding.saveButton.text = getString(R.string.save_button)
            binding.saveButton.icon = null
            binding.saveButton.setBackgroundColor(getColor(R.color.vibe_green))
            binding.saveButton.setTextColor(getColor(R.color.white))
            binding.saveButton.strokeWidth = 0
        }
    }

    private fun bindDetail(detail: com.karabotshivhase.vibe.venue.VenueDetail) {
        binding.nameTextView.text = detail.name

        val address = detail.address
        binding.addressTextView.text = if (address.isNullOrBlank()) {
            getString(R.string.address_not_listed)
        } else {
            address
        }

        val hoursClean = HoursFormatter.clean(detail.openingHours)
        binding.hoursTextView.text = hoursClean ?: getString(R.string.hours_not_listed)

        populateVibeBars(detail.tags)
        populateAmenities(detail.tags.map { it.name })

        binding.openInMapsButton.setOnClickListener {
            openMaps(detail.latitude, detail.longitude, detail.name)
        }
    }

    private fun bindOfflineCopy(space: com.karabotshivhase.vibe.data.favorites.SavedSpace) {
        binding.nameTextView.text = space.name

        val address = space.address
        binding.addressTextView.text = if (address.isNullOrBlank()) {
            getString(R.string.address_not_listed)
        } else {
            address
        }

        val hoursClean = HoursFormatter.clean(space.openingHours)
        binding.hoursTextView.text = hoursClean ?: getString(R.string.hours_not_listed)

        val fakeVotes = space.tags.map { com.karabotshivhase.vibe.venue.TagVote(it, 10) }
        populateVibeBars(fakeVotes)
        populateAmenities(space.tags)

        binding.openInMapsButton.setOnClickListener {
            openMaps(space.latitude, space.longitude, space.name)
        }
    }

    private fun populateVibeBars(tags: List<com.karabotshivhase.vibe.venue.TagVote>) {
        binding.vibeBarsContainer.removeAllViews()
        val totalVotes = tags.sumOf { it.voteCount }.coerceAtLeast(1)
        val samplePercents = listOf(90, 75, 30)

        for ((index, tagVote) in tags.withIndex()) {
            val label = moodLabelRes(tagVote.name)?.let { getString(it) } ?: tagVote.name
            val percent = if (index < samplePercents.size) samplePercents[index] else ((tagVote.voteCount * 100) / totalVotes).coerceIn(10, 95)

            val rowView = layoutInflater.inflate(R.layout.item_vibe_bar, binding.vibeBarsContainer, false)
            val nameTv = rowView.findViewById<TextView>(R.id.barNameTextView)
            val percentTv = rowView.findViewById<TextView>(R.id.barPercentTextView)
            val progressBar = rowView.findViewById<ProgressBar>(R.id.barProgressBar)

            nameTv.text = label
            percentTv.text = "$percent%"
            progressBar.progress = percent

            if (index > 0) {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (16 * resources.displayMetrics.density).toInt()
                }
                binding.vibeBarsContainer.addView(rowView, params)
            } else {
                binding.vibeBarsContainer.addView(rowView)
            }
        }
    }

    private fun populateAmenities(tags: List<String>) {
        binding.chipGroupTags.removeAllViews()
        val defaultAmenities = listOf("Power Outlets", "Fast Wi-Fi", "Quiet Zone", "Wheelchair Acc.")
        val amenityList = if (tags.isNotEmpty()) {
            tags.map { tagName -> moodLabelRes(tagName)?.let { getString(it) } ?: tagName } + defaultAmenities.take(2)
        } else {
            defaultAmenities
        }

        for (amenity in amenityList.distinct()) {
            val chip = Chip(this).apply {
                text = amenity
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.white)
                setTextColor(getColor(R.color.vibe_green))
                chipStrokeColor = getColorStateList(R.color.vibe_green)
                chipStrokeWidth = 1 * resources.displayMetrics.density
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun openMaps(latitude: Double, longitude: Double, name: String) {
        try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedName)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.maps_not_available, Snackbar.LENGTH_LONG).show()
        }
    }
}
