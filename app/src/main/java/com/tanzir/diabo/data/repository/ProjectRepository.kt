package com.tanzir.diabo.data.repository

import com.tanzir.diabo.data.filesystem.FileManager
import com.tanzir.diabo.data.local.dao.ProjectDao
import com.tanzir.diabo.data.local.dao.ProjectFileDao
import com.tanzir.diabo.data.local.entity.BuildStatus
import com.tanzir.diabo.data.local.entity.FileType
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.data.local.entity.ProjectFile
import com.tanzir.diabo.data.local.entity.inferFileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class DiaBoResult<out T> {
    data class Success<T>(val data: T) : DiaBoResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : DiaBoResult<Nothing>()
}

private const val TEMPLATE_ACTIVITY_JAVA = """package {{PACKAGE}};

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
"""

private const val TEMPLATE_LAYOUT_XML = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello, DiaBo!"
        android:textSize="20sp" />

</LinearLayout>
"""

@Singleton
class ProjectRepository @Inject constructor(
    private val fileManager: FileManager,
    private val projectDao: ProjectDao,
    private val projectFileDao: ProjectFileDao
) {

    fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()
    fun observeRecentProjects(limit: Int = 5): Flow<List<Project>> = projectDao.observeRecent(limit)
    fun searchProjects(query: String): Flow<List<Project>> = projectDao.search(query)
    fun observeProjectFiles(projectId: String): Flow<List<ProjectFile>> =
        projectFileDao.observeFiles(projectId)

    fun observeTrash(projectId: String): Flow<List<ProjectFile>> =
        projectFileDao.observeTrash(projectId)

    suspend fun restoreFile(file: ProjectFile) = withContext(Dispatchers.IO) {
        projectFileDao.restore(file.id)
    }

    suspend fun createProject(
        displayName: String,
        template: com.tanzir.diabo.templates.ProjectTemplate? = null
    ): DiaBoResult<Project> = withContext(Dispatchers.IO) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return@withContext DiaBoResult.Error("Project name can't be empty")

        fileManager.ensureRootStructure().onFailure {
            return@withContext DiaBoResult.Error("Storage isn't ready: ${it.message}", it)
        }

        val folderName = sanitizeFolderName(trimmed)
        if (folderName.isEmpty()) {
            return@withContext DiaBoResult.Error("Project name must contain at least one letter/number")
        }
        if (projectDao.folderNameExists(folderName)) {
            return@withContext DiaBoResult.Error("A project named '$trimmed' already exists")
        }

        val packageName = "com.diabo.${folderName.lowercase()}"

        val skeletonResult = fileManager.createProjectSkeleton(folderName)
        val projectDir = skeletonResult.getOrElse {
            return@withContext DiaBoResult.Error("Couldn't create project folder: ${it.message}", it)
        }

        // Seed default MainActivity.java + activity_main.xml so the IDE never opens empty/broken.
        val javaFile = File(projectDir, "${FileManager.SRC_FOLDER}/MainActivity.java")
        val xmlFile = File(projectDir, "${FileManager.RES_LAYOUT_FOLDER}/activity_main.xml")

        val javaSource = (template?.javaContent ?: TEMPLATE_ACTIVITY_JAVA).replace("{{PACKAGE}}", packageName)
        val xmlSource = template?.xmlContent ?: TEMPLATE_LAYOUT_XML

        val javaWrite = fileManager.safeWrite(javaFile, javaSource)
        val xmlWrite = fileManager.safeWrite(xmlFile, xmlSource)

        if (javaWrite.isFailure || xmlWrite.isFailure) {
            fileManager.deleteRecursively(projectDir) // rollback partial project on failure
            return@withContext DiaBoResult.Error("Couldn't write starter files for '$trimmed'")
        }

        val now = System.currentTimeMillis()
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            folderName = folderName,
            packageName = packageName,
            createdAt = now,
            lastModified = now,
            lastBuildStatus = BuildStatus.NONE
        )
        projectDao.upsert(project)

        // Index the two seed files in Room so the file explorer has data immediately.
        indexFile(project.id, javaFile, projectDir)
        indexFile(project.id, xmlFile, projectDir)

        DiaBoResult.Success(project)
    }

    suspend fun renameProject(project: Project, newDisplayName: String): DiaBoResult<Project> =
        withContext(Dispatchers.IO) {
            val trimmed = newDisplayName.trim()
            if (trimmed.isEmpty()) return@withContext DiaBoResult.Error("Name can't be empty")
            val updated = project.copy(name = trimmed, lastModified = System.currentTimeMillis())
            projectDao.update(updated)
            DiaBoResult.Success(updated)
        }

    suspend fun deleteProject(project: Project): DiaBoResult<Unit> = withContext(Dispatchers.IO) {
        fileManager.deleteRecursively(fileManager.projectDir(project.folderName)).onFailure {
            return@withContext DiaBoResult.Error("Couldn't delete project files: ${it.message}", it)
        }
        projectDao.delete(project)
        DiaBoResult.Success(Unit)
    }

    suspend fun duplicateProject(project: Project): DiaBoResult<Project> = withContext(Dispatchers.IO) {
        val sourceDir = fileManager.projectDir(project.folderName)
        if (!sourceDir.exists()) return@withContext DiaBoResult.Error("Original project folder is missing")

        var newFolderName = "${project.folderName}_copy"
        var i = 1
        while (projectDao.folderNameExists(newFolderName)) {
            newFolderName = "${project.folderName}_copy$i"
            i++
        }
        val destDir = fileManager.projectDir(newFolderName)
        runCatching { sourceDir.copyRecursively(destDir, overwrite = false) }
            .onFailure { return@withContext DiaBoResult.Error("Copy failed: ${it.message}", it) }

        val now = System.currentTimeMillis()
        val newProject = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} Copy",
            folderName = newFolderName,
            createdAt = now,
            lastModified = now
        )
        projectDao.upsert(newProject)
        reindexProject(newProject)
        DiaBoResult.Success(newProject)
    }

    // ---- File operations (used by the Code IDE sidebar) ----

    suspend fun createFile(project: Project, relativeDir: String, fileName: String): DiaBoResult<ProjectFile> =
        withContext(Dispatchers.IO) {
            val projectDir = fileManager.projectDir(project.folderName)
            val target = File(File(projectDir, relativeDir), fileName)
            if (target.exists()) return@withContext DiaBoResult.Error("'$fileName' already exists here")

            val initialContent = when {
                fileName.endsWith(".xml") -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                fileName.endsWith(".java") -> "// $fileName\n"
                else -> ""
            }
            fileManager.safeWrite(target, initialContent).onFailure {
                return@withContext DiaBoResult.Error("Couldn't create file: ${it.message}", it)
            }
            val entity = indexFile(project.id, target, projectDir)
            touchProject(project)
            DiaBoResult.Success(entity)
        }

    suspend fun createFolder(project: Project, relativeDir: String, folderName: String): DiaBoResult<Unit> =
        withContext(Dispatchers.IO) {
            val projectDir = fileManager.projectDir(project.folderName)
            val target = File(File(projectDir, relativeDir), folderName)
            if (target.exists()) return@withContext DiaBoResult.Error("'$folderName' already exists here")
            if (!target.mkdirs()) return@withContext DiaBoResult.Error("Couldn't create folder")
            touchProject(project)
            DiaBoResult.Success(Unit)
        }

    suspend fun saveFileContent(project: Project, file: ProjectFile, content: String): DiaBoResult<Unit> =
        withContext(Dispatchers.IO) {
            val projectDir = fileManager.projectDir(project.folderName)
            val target = File(projectDir, file.relativePath)
            fileManager.safeWrite(target, content).onFailure {
                return@withContext DiaBoResult.Error("Auto-save failed: ${it.message}", it)
            }
            projectFileDao.update(file.copy(lastModified = System.currentTimeMillis()))
            touchProject(project)
            DiaBoResult.Success(Unit)
        }

    suspend fun readFileContent(project: Project, file: ProjectFile): DiaBoResult<String> =
        withContext(Dispatchers.IO) {
            val projectDir = fileManager.projectDir(project.folderName)
            val target = File(projectDir, file.relativePath)
            fileManager.safeRead(target).fold(
                onSuccess = { DiaBoResult.Success(it) },
                onFailure = { DiaBoResult.Error("Couldn't open file: ${it.message}", it) }
            )
        }

    suspend fun renameFile(project: Project, file: ProjectFile, newName: String): DiaBoResult<ProjectFile> =
        withContext(Dispatchers.IO) {
            val projectDir = fileManager.projectDir(project.folderName)
            val target = File(projectDir, file.relativePath)
            val renamed = fileManager.renameFile(target, newName).getOrElse {
                return@withContext DiaBoResult.Error(it.message ?: "Rename failed", it)
            }
            val updated = file.copy(
                name = newName,
                relativePath = renamed.relativeTo(projectDir).path,
                type = inferFileType(newName),
                lastModified = System.currentTimeMillis()
            )
            projectFileDao.update(updated)
            DiaBoResult.Success(updated)
        }

    suspend fun duplicateFile(project: Project, file: ProjectFile): DiaBoResult<ProjectFile> =
        withContext(Dispatchers.IO) {
            val projectDir = fileManager.projectDir(project.folderName)
            val target = File(projectDir, file.relativePath)
            val dup = fileManager.duplicateFile(target).getOrElse {
                return@withContext DiaBoResult.Error(it.message ?: "Duplicate failed", it)
            }
            val entity = indexFile(project.id, dup, projectDir)
            DiaBoResult.Success(entity)
        }

    /** Soft-delete: recoverable for 24h from the trash before a background job purges it. */
    suspend fun softDeleteFile(file: ProjectFile): DiaBoResult<Unit> = withContext(Dispatchers.IO) {
        projectFileDao.softDelete(file.id, System.currentTimeMillis())
        DiaBoResult.Success(Unit)
    }

    suspend fun setPinned(file: ProjectFile, pinned: Boolean) = withContext(Dispatchers.IO) {
        projectFileDao.setPinned(file.id, pinned)
    }

    suspend fun purgeExpiredTrash(retentionMillis: Long = 24 * 60 * 60 * 1000) = withContext(Dispatchers.IO) {
        projectFileDao.purgeExpiredTrash(System.currentTimeMillis() - retentionMillis)
    }

    // ---- internal helpers ----

    private suspend fun touchProject(project: Project) {
        projectDao.update(project.copy(lastModified = System.currentTimeMillis()))
    }

    private suspend fun indexFile(projectId: String, file: File, projectDir: File): ProjectFile {
        val entity = ProjectFile(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            relativePath = file.relativeTo(projectDir).path,
            name = file.name,
            type = inferFileType(file.name),
            lastModified = System.currentTimeMillis()
        )
        projectFileDao.upsert(entity)
        return entity
    }

    /** Re-walks a project folder and rebuilds its Room file index (used after duplicate/import). */
    private suspend fun reindexProject(project: Project) {
        val projectDir = fileManager.projectDir(project.folderName)
        projectDir.walkTopDown()
            .filter { it.isFile && it.name != FileManager.PROJECT_META_FILE }
            .forEach { indexFile(project.id, it, projectDir) }
    }

    fun projectFolder(project: Project): File = fileManager.projectDir(project.folderName)

    private fun sanitizeFolderName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9_-]"), "_").trim('_')
}
