package com.tanzir.diabo.data.repository

import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs [ProjectRepository.purgeExpiredTrash] once a day. This is the piece that makes
 * the soft-delete-then-recoverable-for-24h promise (see ProjectFile.isDeleted doc comment,
 * FileExplorerSidebar's delete confirmation) actually true — without this scheduled job,
 * trashed files would accumulate forever instead of ever being purged.
 *
 * Scheduled from MainActivity.onCreate via [TrashPurgeScheduler.schedule] using
 * enqueueUniquePeriodicWork, so re-launching the app never creates duplicate jobs.
 */
@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted params: WorkerParameters,
    private val repository: ProjectRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.purgeExpiredTrash()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
