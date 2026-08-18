package com.tanzir.diabo.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class WorkflowDispatchBody(
    val ref: String,
    val inputs: Map<String, String>
)

data class WorkflowRunsResponse(
    val total_count: Int,
    val workflow_runs: List<WorkflowRun>
)

data class WorkflowRun(
    val id: Long,
    val name: String?,
    val status: String,       // queued, in_progress, completed
    val conclusion: String?,  // success, failure, cancelled, null while running
    val html_url: String,
    val created_at: String,
    val run_started_at: String?
)

data class ArtifactsResponse(
    val total_count: Int,
    val artifacts: List<Artifact>
)

data class Artifact(
    val id: Long,
    val name: String,
    val archive_download_url: String,
    val expired: Boolean
)

interface GitHubApiService {

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow") workflowFile: String,
        @Body body: WorkflowDispatchBody
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/workflows/{workflow}/runs")
    suspend fun listWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow") workflowFile: String,
        @Query("per_page") perPage: Int = 10,
        @Query("event") event: String = "workflow_dispatch"
    ): Response<WorkflowRunsResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun listArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<ArtifactsResponse>

    @GET
    @Streaming
    suspend fun downloadArtifact(@Url url: String): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/jobs")
    suspend fun listJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<ResponseBody>
}
