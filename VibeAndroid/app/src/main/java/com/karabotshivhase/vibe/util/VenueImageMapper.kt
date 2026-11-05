package com.karabotshivhase.vibe.util

import androidx.annotation.DrawableRes
import com.karabotshivhase.vibe.R

object VenueImageMapper {

    @DrawableRes
    fun imageForVenue(mood: String?, venueId: Int): Int {
        val normalizedMood = mood?.trim()?.lowercase() ?: ""
        val index = (kotlin.math.abs(venueId) % 3)

        return when {
            normalizedMood.contains("quiet") || normalizedMood.contains("focus") -> {
                when (index) {
                    0 -> R.drawable.quiet_focus1
                    1 -> R.drawable.quiet_focus2
                    else -> R.drawable.quiet_focus3
                }
            }
            normalizedMood.contains("social") -> {
                when (index) {
                    0 -> R.drawable.social1
                    1 -> R.drawable.social2
                    else -> R.drawable.social3
                }
            }
            normalizedMood.contains("creative") -> {
                when (index) {
                    0 -> R.drawable.creative1
                    1 -> R.drawable.creative2
                    else -> R.drawable.creative_3
                }
            }
            normalizedMood.contains("wifi") || normalizedMood.contains("wi-fi") || normalizedMood.contains("cheap") -> {
                when (index) {
                    0 -> R.drawable.wifi
                    1 -> R.drawable.wifi2
                    else -> R.drawable.wifi3
                }
            }
            else -> {
                when (index) {
                    0 -> R.drawable.quiet_focus1
                    1 -> R.drawable.social1
                    else -> R.drawable.creative1
                }
            }
        }
    }
}
