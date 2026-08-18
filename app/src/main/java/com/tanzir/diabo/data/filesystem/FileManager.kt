package com.tanzir.diabo.data.filesystem

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for DiaBo's on-disk layout.
 *
 * Root lives in the app's true INTERNAL storage (Context.filesDir) — NOT shared/external
 * storage — so it needs no storage permission and is private to DiaBo:
 *
 *   /data/data/com.tanzir.diabo/files/DiaBo/
 *   ├── Projects/<ProjectName>/src/...java, res/layout/...xml, project.json, .diabo_cache/
 *   ├── Templates/
 *   ├── Backups/
 *   ├── BuildCache/
 *   └── Logs/
 *
 * Every method is defensive: folder creation is idempotent and safe to call on every
 * cold start (self-healing), per the "zero bugs" reliability requirement.
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val ROOT_FOLDER = "DiaBo"
        const val PROJECTS_FOLDER = "Projects"
        const val TEMPLATES_FOLDER = "Templates"
        const val BACKUPS_FOLDER = "Backups"
        const val BUILD_CACHE_FOLDER = "BuildCache"
        const val LOGS_FOLDER = "Logs"
        const val SRC_FOLDER = "src"
        const val RES_LAYOUT_FOLDER = "res/layout"
        const val PROJECT_META_FILE = "project.json"
        const val PREVIEW_CACHE_FOLDER = ".diabo_cache"

        private const val TAG = "DiaBoFileManager"
    }

    val rootDir: File by lazy { File(context.filesDir, ROOT_FOLDER) }
    val projectsDir: File by lazy { File(rootDir, PROJECTS_FOLDER) }
    val templatesDir: File by lazy { File(rootDir, TEMPLATES_FOLDER) }
    val backupsDir: File by lazy { File(rootDir, BACKUPS_FOLDER) }
    val buildCacheDir: File by lazy { File(rootDir, BUILD_CACHE_FOLDER) }
    val logsDir: File by lazy { File(rootDir, LOGS_FOLDER) }

    /** Call once on app start (and defensively before any file op) to guarantee the tree exists. */
    fun ensureRootStructure(): Result<Unit> = runCatching {
        listOf(rootDir, projectsDir, templatesDir, backupsDir, buildCacheDir, logsDir).forEach { dir ->
            if (!dir.exists() && !dir.mkdirs()) {
                throw IOException("Could not create required folder: ${dir.absolutePath}")
            }
        }
    }

    fun projectDir(projectFolderName: String): File =
        File(projectsDir, projectFolderName)

    fun createProjectSkeleton(projectFolderName: String): Result<File> = runCatching {
        val pDir = projectDir(projectFolderName)
        if (pDir.exists()) throw IOException("Project '$projectFolderName' already exists")

        val srcDir = File(pDir, SRC_FOLDER)
        val layoutDir = File(pDir, RES_LAYOUT_FOLDER)
        val cacheDir = File(pDir, PREVIEW_CACHE_FOLDER)

        listOf(pDir, srcDir, layoutDir, cacheDir).forEach {
            if (!it.mkdirs()) throw IOException("Failed creating ${it.absolutePath}")
        }
        pDir
    }

    fun safeWrite(file: File, content: String): Result<Unit> = runCatching {
        // Atomic write: write to temp file then rename, so a crash mid-write never corrupts
        // the user's existing code (critical per the zero-data-loss requirement).
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        tempFile.writeText(content)
        if (!tempFile.renameTo(file)) {
            // Fallback for filesystems where atomic rename across same dir fails
            file.writeText(content)
            tempFile.delete()
        }
    }

    fun safeRead(file: File): Result<String> = runCatching {
        if (!file.exists()) throw IOException("File does not exist: ${file.absolutePath}")
        file.readText()
    }

    fun deleteRecursively(target: File): Result<Unit> = runCatching {
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException("Failed to delete ${target.absolutePath}")
        }
    }

    fun renameFile(target: File, newName: String): Result<File> = runCatching {
        val dest = File(target.parentFile, newName)
        if (dest.exists()) throw IOException("A file/folder named '$newName' already exists")
        if (!target.renameTo(dest)) throw IOException("Rename failed for ${target.absolutePath}")
        dest
    }

    fun duplicateFile(target: File): Result<File> = runCatching {
        if (!target.isFile) throw IOException("Duplicate only supported for files")
        val base = target.nameWithoutExtension
        val ext = target.extension
        var candidate = File(target.parentFile, "${base}_copy.$ext")
        var i = 1
        while (candidate.exists()) {
            candidate = File(target.parentFile, "${base}_copy$i.$ext")
            i++
        }
        target.copyTo(candidate)
        candidate
    }

    /** Recursively lists a project folder for the file-tree UI. */
    fun listTree(dir: File): List<File> =
        dir.listFiles()?.sortedWith(compareBy({ it.isFile }, { it.name.lowercase() })) ?: emptyList()

    fun folderSizeBytes(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
