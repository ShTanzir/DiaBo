package com.tanzir.diabo.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language toggle (English/Bangla), independent of the device's system locale.
 * Persisted automatically by AppCompatDelegate across process restarts as of appcompat 1.6+.
 */
object LanguageManager {

    fun setBangla() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("bn"))
    }

    fun setEnglish() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    fun isBangla(): Boolean =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith("bn")
}
