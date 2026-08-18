package com.tanzir.diabo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileType { JAVA, XML, GRADLE, FOLDER, OTHER }

@Entity(tableName = "project_files")
data class ProjectFile(
    @PrimaryKey val id: String,
    val projectId: String,
    val relativePath: String,      // path relative to the project's root folder
    val name: String,
    val type: FileType,
    val isPinned: Boolean = false,
    val lastModified: Long,
    val isDeleted: Boolean = false, // soft-trash flag, purged after 24h by a WorkManager job
    val deletedAt: Long? = null
)

fun inferFileType(fileName: String): FileType = when {
    fileName.endsWith(".java") -> FileType.JAVA
    fileName.endsWith(".xml") -> FileType.XML
    fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts") -> FileType.GRADLE
    !fileName.contains(".") -> FileType.FOLDER
    else -> FileType.OTHER
}
