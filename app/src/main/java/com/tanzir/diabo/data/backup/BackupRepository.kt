package com.tanzir.diabo.data.backup

import android.content.Context
import android.net.Uri
import com.tanzir.diabo.data.filesystem.FileManager
import com.tanzir.diabo.data.local.dao.ProjectDao
import com.tanzir.diabo.data.local.dao.ProjectFileDao
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.data.local.entity.inferFileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class Success(val projectsRestored: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

/**
 * Exports every project folder (as-is, including project.json metadata) into a single
 * ZIP the user picks a destination for via SAF, and restores from a previously exported
 * ZIP — re-indexing Room from what's actually on disk rather than trusting the ZIP's
 * manifest blindly, so a hand-edited backup can't corrupt the app's database.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager,
    private val projectDao: ProjectDao,
    private val projectFileDao: ProjectFileDao
) {

    suspend fun exportAll(destinationUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val projectsDir = fileManager.projectsDir
            if (!projectsDir.exists() || projectsDir.listFiles().isNullOrEmpty()) {
                return@withContext BackupResult.Error("No projects to back up yet")
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    projectsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val entryName = file.relativeTo(fileManager.rootDir).path
                        zip.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: return@withContext BackupResult.Error("Couldn't open the chosen file for writing")

            val count = projectDao.count()
            BackupResult.Success(count)
        } catch (e: Exception) {
            BackupResult.Error("Backup failed: ${e.message}")
        }
    }

    suspend fun restoreFrom(sourceUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            fileManager.ensureRootStructure()
            var extractedFiles = 0

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            // entryName is rootDir-relative, e.g. "Projects/MyApp/src/MainActivity.java"
                            val outFile = File(fileManager.rootDir, entry.name)
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out -> zip.copyTo(out) }
                            extractedFiles++
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext BackupResult.Error("Couldn't open the chosen backup file")

            if (extractedFiles == 0) return@withContext BackupResult.Error("Backup file was empty or invalid")

            val restoredCount = reindexAllProjectsFromDisk()
            BackupResult.Success(restoredCount)
        } catch (e: Exception) {
            BackupResult.Error("Restore failed: ${e.message}")
        }
    }

    /** Walks Projects/ on disk and rebuilds Room rows for any folder not already indexed. */
    private suspend fun reindexAllProjectsFromDisk(): Int {
        val projectFolders = fileManager.projectsDir.listFiles { f -> f.isDirectory } ?: return 0
        var restored = 0

        for (folder in projectFolders) {
            if (projectDao.folderNameExists(folder.name)) continue // already known, don't duplicate

            val now = System.currentTimeMillis()
            val project = Project(
                id = UUID.randomUUID().toString(),
                name = folder.name,
                folderName = folder.name,
                packageName = "com.diabo.${folder.name.lowercase()}",
                createdAt = now,
                lastModified = folder.lastModified().takeIf { it > 0 } ?: now
            )
            projectDao.upsert(project)

            folder.walkTopDown()
                .filter { it.isFile && it.name != FileManager.PROJECT_META_FILE }
                .forEach { file ->
                    projectFileDao.upsert(
                        com.tanzir.diabo.data.local.entity.ProjectFile(
                            id = UUID.randomUUID().toString(),
                            projectId = project.id,
                            relativePath = file.relativeTo(folder).path,
                            name = file.name,
                            type = inferFileType(file.name),
                            lastModified = file.lastModified()
                        )
                    )
                }
            restored++
        }
        return restored
    }
}
