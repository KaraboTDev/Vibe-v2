package com.karabotshivhase.vibe.ui.results

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.databinding.ItemVenueBinding
import com.karabotshivhase.vibe.venue.Venue
import com.karabotshivhase.vibe.util.DistanceFormatter
import com.karabotshivhase.vibe.util.moodLabelRes

class VenuesAdapter(
    private val onVenueClick: (Venue) -> Unit,
) : ListAdapter<Venue, VenuesAdapter.VenueViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        val binding = ItemVenueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VenueViewHolder(binding, onVenueClick)
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VenueViewHolder(
        private val binding: ItemVenueBinding,
        private val onVenueClick: (Venue) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(venue: Venue) {
            val context = binding.root.context
            binding.nameTextView.text = venue.name
            val distanceStr = DistanceFormatter.format(venue.distanceMeters)
            binding.distanceTextView.text = "$distanceStr away"

            val tagsStr = if (venue.tags.isNotEmpty()) {
                venue.tags.joinToString(" · ") { tag ->
                    moodLabelRes(tag)?.let { context.getString(it) } ?: tag
                }
            } else {
                ""
            }
            binding.tagsTextView.text = tagsStr
            binding.tagsTextView.visibility = if (tagsStr.isEmpty()) View.GONE else View.VISIBLE

            binding.ratingTextView.text = "4.8"

            val combinedDesc = context.getString(
                R.string.venue_item_description,
                venue.name,
                distanceStr,
                if (venue.tags.isNotEmpty()) venue.tags.joinToString(" and ") else ""
            )
            binding.root.contentDescription = combinedDesc

            binding.root.setOnClickListener {
                onVenueClick(venue)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Venue>() {
            override fun areItemsTheSame(oldItem: Venue, newItem: Venue): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Venue, newItem: Venue): Boolean {
                return oldItem == newItem
            }
        }
    }
}
