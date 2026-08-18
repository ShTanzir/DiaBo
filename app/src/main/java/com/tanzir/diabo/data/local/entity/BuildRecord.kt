package com.tanzir.diabo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CloudBuildStatus { QUEUED, BUILDING, INSTALLING, CAPTURING, SUCCESS, FAILED, CANCELLED }

@Entity(tableName = "build_records")
data class BuildRecord(
    @PrimaryKey val buildId: String,
    val projectId: String,
    val status: CloudBuildStatus,
    val triggeredAt: Long,
    val completedAt: Long? = null,
    val githubRunId: Long? = null,
    val githubRunUrl: String? = null,
    val apkPath: String? = null,
    val screenshotPath: String? = null,
    val logSummary: String? = null,
    val errorMessage: String? = null
)
