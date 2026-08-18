package com.tanzir.diabo.data.build

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tanzir.diabo.data.local.dao.BuildRecordDao
import com.tanzir.diabo.data.local.entity.CloudBuildStatus
import com.tanzir.diabo.util.BuildNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs [CloudBuildRepository.pollAndCollect] as background work so a Real Build survives
 * the user leaving the Code IDE screen or backgrounding the app entirely. Expedited so it
 * starts promptly even under Doze, per the PRD's WAKE_LOCK/FOREGROUND_SERVICE permissions.
 */
@HiltWorker
class BuildPollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CloudBuildRepository,
    private val buildRecordDao: BuildRecordDao,
    private val notifier: BuildNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val buildId = inputData.getString(KEY_BUILD_ID) ?: return Result.failure()
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure()

        return try {
            repository.pollAndCollect(buildId, projectId)

            val finalRecord = buildRecordDao.getById(buildId)
            when (finalRecord?.status) {
                CloudBuildStatus.SUCCESS -> notifier.notifyBuildSuccess(projectId, buildId)
                CloudBuildStatus.FAILED -> notifier.notifyBuildFailed(projectId, buildId, finalRecord.errorMessage)
                else -> Unit
            }
            Result.success()
        } catch (e: Exception) {
            notifier.notifyBuildFailed(projectId, buildId, e.message)
            Result.failure()
        }
    }

    companion object {
        const val KEY_BUILD_ID = "build_id"
        const val KEY_PROJECT_ID = "project_id"
    }
}
