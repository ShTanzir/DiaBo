package com.tanzir.diabo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BuildStatus { NONE, SUCCESS, FAILED, IN_PROGRESS }

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val folderName: String,        // sanitized on-disk folder name, may differ from display name
    val packageName: String,
    val createdAt: Long,
    val lastModified: Long,
    val lastBuildStatus: BuildStatus = BuildStatus.NONE,
    val thumbnailPath: String? = null,
    val schemaVersion: Int = 1     // for forward-compatible project.json migrations
)
