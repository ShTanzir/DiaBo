package com.tanzir.diabo.data.repository

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashPurgeScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        // KEEP, not REPLACE: re-scheduling on every app launch must not reset the
        // existing job's timer, or trash would never actually reach its 24h purge window.
        workManager.enqueueUniquePeriodicWork(
            "diabo_trash_purge",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
