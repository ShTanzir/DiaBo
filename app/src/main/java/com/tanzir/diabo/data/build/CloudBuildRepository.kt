package com.tanzir.diabo.data.build

import com.tanzir.diabo.data.filesystem.FileManager
import com.tanzir.diabo.data.local.dao.BuildRecordDao
import com.tanzir.diabo.data.local.entity.CloudBuildStatus
import com.tanzir.diabo.data.local.entity.BuildRecord
import com.tanzir.diabo.data.remote.GitHubApiService
import com.tanzir.diabo.data.remote.GitHubConfigStore
import com.tanzir.diabo.data.remote.WorkflowDispatchBody
import com.tanzir.diabo.data.remote.WorkflowRun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class TriggerBuildResult {
    data class Started(val buildId: String) : TriggerBuildResult()
    data class NotConfigured(val message: String = "Connect GitHub in Settings first") : TriggerBuildResult()
    data class Error(val message: String) : TriggerBuildResult()
}

private const val POLL_INTERVAL_MS = 15_000L
private const val MAX_POLL_ATTEMPTS = 40           // ~10 minutes at 15s intervals, matches the workflow's own 12-min hard cap
private const val RUN_MATCH_WINDOW_MS = 5 * 60_000L // only consider runs started within 5 min of trigger

@Singleton
class CloudBuildRepository @Inject constructor(
    private val api: GitHubApiService,
    private val configStore: GitHubConfigStore,
    private val buildRecordDao: BuildRecordDao,
    private val fileManager: FileManager
) {
    fun observeBuildsForProject(projectId: String) = buildRecordDao.observeForProject(projectId)
    fun observeBuild(buildId: String) = buildRecordDao.observeById(buildId)

    /**
     * Kicks off a cloud build and immediately returns a buildId — the actual GitHub round-trip
     * (dispatch -> poll -> download) runs via [pollAndCollect], intended to be launched from a
     * WorkManager worker so it survives the screen closing.
     */
    suspend fun triggerBuild(projectId: String, javaCode: String, xmlCode: String): TriggerBuildResult =
        withContext(Dispatchers.IO) {
            val config = configStore.load() ?: return@withContext TriggerBuildResult.NotConfigured()

            val buildId = UUID.randomUUID().toString().take(12)
            val triggeredAt = System.currentTimeMillis()

            buildRecordDao.upsert(
                BuildRecord(buildId = buildId, projectId = projectId, status = CloudBuildStatus.QUEUED, triggeredAt = triggeredAt)
            )

            val javaB64 = android.util.Base64.encodeToString(javaCode.toByteArray(), android.util.Base64.NO_WRAP)
            val xmlB64 = android.util.Base64.encodeToString(xmlCode.toByteArray(), android.util.Base64.NO_WRAP)

            val response = runCatching {
                api.dispatchWorkflow(
                    owner = config.owner,
                    repo = config.repo,
                    workflowFile = config.workflowFile,
                    body = WorkflowDispatchBody(
                        ref = config.branch,
                        inputs = mapOf(
                            "java_code_b64" to javaB64,
                            "xml_code_b64" to xmlB64,
                            "build_id" to buildId
                        )
                    )
                )
            }.getOrElse {
                markFailed(buildId, "Network error: ${it.message}")
                return@withContext TriggerBuildResult.Error("Couldn't reach GitHub: ${it.message}")
            }

            if (!response.isSuccessful) {
                val msg = interpretGitHubError(response.code())
                markFailed(buildId, msg)
                return@withContext TriggerBuildResult.Error(msg)
            }

            TriggerBuildResult.Started(buildId)
        }

    /**
     * Polls GitHub until the matching run completes (or times out), then downloads artifacts.
     * Designed to be called from a background worker — every state transition is persisted to
     * Room immediately so the UI (and a relaunched app) always reflects the latest known status.
     */
    suspend fun pollAndCollect(buildId: String, projectId: String) = withContext(Dispatchers.IO) {
        val config = configStore.load() ?: run { markFailed(buildId, "GitHub not configured"); return@withContext }
        val record = buildRecordDao.getById(buildId) ?: return@withContext

        updateStatus(buildId, CloudBuildStatus.BUILDING)

        var matchedRun: WorkflowRun? = null
        var attempts = 0

        while (attempts < MAX_POLL_ATTEMPTS && matchedRun == null) {
            delay(if (attempts == 0) 5_000L else POLL_INTERVAL_MS) // small initial delay before first check
            attempts++

            val runsResponse = runCatching {
                api.listWorkflowRuns(config.owner, config.repo, config.workflowFile)
            }.getOrNull()

            val runs = runsResponse?.takeIf { it.isSuccessful }?.body()?.workflow_runs.orEmpty()
            matchedRun = runs.firstOrNull { run ->
                val startedAt = parseIso8601(run.created_at) ?: return@firstOrNull false
                startedAt >= record.triggeredAt - 15_000 && startedAt <= record.triggeredAt + RUN_MATCH_WINDOW_MS
            }
        }

        val run = matchedRun ?: run {
            markFailed(buildId, "Couldn't find the build on GitHub — check Settings → GitHub Integration")
            return@withContext
        }

        buildRecordDao.upsert(
            record.copy(status = CloudBuildStatus.BUILDING, githubRunId = run.id, githubRunUrl = run.html_url)
        )

        // Keep polling this specific run until GitHub reports it "completed".
        var finalRun = run
        var waitAttempts = 0
        while (finalRun.status != "completed" && waitAttempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            waitAttempts++
            val refreshed = runCatching { api.listWorkflowRuns(config.owner, config.repo, config.workflowFile) }
                .getOrNull()?.body()?.workflow_runs?.firstOrNull { it.id == run.id }
            if (refreshed != null) finalRun = refreshed
        }

        if (finalRun.status != "completed") {
            markFailed(buildId, "Build timed out waiting for GitHub Actions")
            return@withContext
        }

        if (finalRun.conclusion != "success") {
            markFailed(buildId, "Build failed on GitHub (conclusion: ${finalRun.conclusion}). Open the run for logs: ${finalRun.html_url}")
            return@withContext
        }

        updateStatus(buildId, CloudBuildStatus.CAPTURING)
        downloadArtifacts(buildId, projectId, config.owner, config.repo, run.id)
    }

    private suspend fun downloadArtifacts(buildId: String, projectId: String, owner: String, repo: String, runId: Long) {
        val artifactsResponse = runCatching { api.listArtifacts(owner, repo, runId) }.getOrNull()
        val artifacts = artifactsResponse?.takeIf { it.isSuccessful }?.body()?.artifacts.orEmpty()

        val apkArtifact = artifacts.firstOrNull { it.name == "apk-$buildId" }
        val screenshotArtifact = artifacts.firstOrNull { it.name == "screenshot-$buildId" }

        if (apkArtifact == null) {
            markFailed(buildId, "Build succeeded but no APK artifact was found")
            return
        }

        val destDir = File(fileManager.buildCacheDir, "$projectId/$buildId").apply { mkdirs() }

        val apkPath = runCatching {
            downloadAndExtractZip(apkArtifact.archive_download_url, destDir, expectedExtension = "apk")
        }.getOrNull()

        val screenshotPath = screenshotArtifact?.let {
            runCatching { downloadAndExtractZip(it.archive_download_url, destDir, expectedExtension = "png") }.getOrNull()
        }

        if (apkPath == null) {
            markFailed(buildId, "Downloaded artifact but couldn't extract the APK")
            return
        }

        val record = buildRecordDao.getById(buildId) ?: return
        buildRecordDao.upsert(
            record.copy(
                status = CloudBuildStatus.SUCCESS,
                completedAt = System.currentTimeMillis(),
                apkPath = apkPath.absolutePath,
                screenshotPath = screenshotPath?.absolutePath
            )
        )
    }

    /** GitHub Actions artifacts are always delivered as a zip, even for a single file. */
    private suspend fun downloadAndExtractZip(url: String, destDir: File, expectedExtension: String): File? {
        val response = api.downloadArtifact(url)
        if (!response.isSuccessful) return null
        val body: ResponseBody = response.body() ?: return null

        var extracted: File? = null
        ZipInputStream(body.byteStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".$expectedExtension")) {
                    val outFile = File(destDir, entry.name)
                    outFile.outputStream().use { out -> zip.copyTo(out) }
                    extracted = outFile
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return extracted
    }

    private suspend fun updateStatus(buildId: String, status: CloudBuildStatus) {
        val record = buildRecordDao.getById(buildId) ?: return
        buildRecordDao.upsert(record.copy(status = status))
    }

    private suspend fun markFailed(buildId: String, message: String) {
        val record = buildRecordDao.getById(buildId)
        if (record != null) {
            buildRecordDao.upsert(
                record.copy(status = CloudBuildStatus.FAILED, completedAt = System.currentTimeMillis(), errorMessage = message)
            )
        }
    }

    private fun interpretGitHubError(code: Int): String = when (code) {
        401 -> "GitHub token is invalid or expired — update it in Settings"
        403 -> "GitHub token doesn't have permission, or rate limit was hit"
        404 -> "Repo or workflow file not found — check owner/repo/workflow name in Settings"
        422 -> "GitHub rejected the request — check the branch name in Settings"
        else -> "GitHub API error ($code)"
    }

    private fun parseIso8601(raw: String): Long? =
        runCatching { java.time.Instant.parse(raw).toEpochMilli() }.getOrNull()
}
