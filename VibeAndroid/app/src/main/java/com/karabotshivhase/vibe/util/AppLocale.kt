package com.karabotshivhase.vibe.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.karabotshivhase.vibe.data.settings.LanguageCodes

object AppLocale {
    fun apply(code: String) {
        val normalized = LanguageCodes.normalize(code)
        val currentLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentLocales != normalized) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
        }
    }
}
