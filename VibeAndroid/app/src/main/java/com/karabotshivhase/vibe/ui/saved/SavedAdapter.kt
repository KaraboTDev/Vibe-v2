package com.karabotshivhase.vibe.ui.saved

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.data.favorites.SavedSpace
import com.karabotshivhase.vibe.databinding.ItemSavedBinding
import com.karabotshivhase.vibe.util.HoursFormatter
import com.karabotshivhase.vibe.util.moodLabelRes

class SavedAdapter(
    private val onSpaceClick: (SavedSpace) -> Unit,
    private val onRemoveClick: (SavedSpace) -> Unit,
) : ListAdapter<SavedSpace, SavedAdapter.SavedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedViewHolder {
        val binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SavedViewHolder(binding, onSpaceClick, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SavedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SavedViewHolder(
        private val binding: ItemSavedBinding,
        private val onSpaceClick: (SavedSpace) -> Unit,
        private val onRemoveClick: (SavedSpace) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(space: SavedSpace) {
            val context = binding.root.context
            binding.nameTextView.text = space.name

            val tagsStr = if (space.tags.isNotEmpty()) {
                space.tags.joinToString(" · ") { tag ->
                    moodLabelRes(tag)?.let { context.getString(it) } ?: tag
                }
            } else {
                ""
            }
            binding.tagsTextView.text = tagsStr
            binding.tagsTextView.visibility = if (tagsStr.isEmpty()) View.GONE else View.VISIBLE

            val hoursClean = HoursFormatter.clean(space.openingHours)
            val hoursStr = hoursClean ?: context.getString(R.string.hours_not_listed)
            binding.hoursTextView.text = hoursStr

            binding.pendingChip.visibility = if (space.isPendingSync) View.VISIBLE else View.GONE

            binding.removeButton.contentDescription = context.getString(
                R.string.remove_saved_description,
                space.name,
            )

            binding.removeButton.setOnClickListener {
                onRemoveClick(space)
            }

            binding.root.setOnClickListener {
                onSpaceClick(space)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SavedSpace>() {
            override fun areItemsTheSame(oldItem: SavedSpace, newItem: SavedSpace): Boolean {
                return oldItem.venueId == newItem.venueId
            }

            override fun areContentsTheSame(oldItem: SavedSpace, newItem: SavedSpace): Boolean {
                return oldItem == newItem
            }
        }
    }
}
