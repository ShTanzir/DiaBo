package com.tanzir.diabo.util

import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Crashlytics so the rest of the app never has to null-check
 * or try/catch Firebase calls directly. If google-services.json wasn't added
 * (see PHASE5_HARDENING.md), [FirebaseApp.initializeApp] never ran, so every
 * method here becomes a safe no-op instead of throwing.
 */
@Singleton
class CrashReporter @Inject constructor() {

    private val isAvailable: Boolean by lazy {
        runCatching { FirebaseApp.getInstance(); true }.getOrDefault(false)
    }

    fun recordException(throwable: Throwable) {
        if (!isAvailable) return
        runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
    }

    fun log(message: String) {
        if (!isAvailable) return
        runCatching { FirebaseCrashlytics.getInstance().log(message) }
    }

    fun setCustomKey(key: String, value: String) {
        if (!isAvailable) return
        runCatching { FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
    }

    /** Call once the user identifies themselves in-app, if ever — DiaBo has no login today. */
    fun setUserId(id: String) {
        if (!isAvailable) return
        runCatching { FirebaseCrashlytics.getInstance().setUserId(id) }
    }
}
