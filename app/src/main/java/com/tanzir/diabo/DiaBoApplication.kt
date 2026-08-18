package com.tanzir.diabo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tanzir.diabo.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * DiaBo Application entry point.
 * Hilt graph root — all ViewModels/Repositories are injected from here.
 * Also configures WorkManager with Hilt's worker factory so BuildPollWorker
 * (Phase 3 Cloud Build) can receive constructor-injected dependencies, and
 * installs a global crash reporter (Phase 5 Hardening).
 */
@HiltAndroidApp
class DiaBoApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        // FileManager self-heals the DiaBo/ root folder structure on every cold start,
        // so a cleared app-data or manually-edited folder never leaves the app in a broken state.
        installGlobalCrashHandler()
    }

    /**
     * Reports every uncaught exception to Crashlytics (a safe no-op if google-services.json
     * hasn't been added yet — see PHASE5_HARDENING.md) before letting the platform's default
     * handler crash the process as usual. This never swallows or masks a crash; it only adds
     * visibility so field crashes surface before a widened rollout.
     */
    private fun installGlobalCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashReporter.recordException(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
