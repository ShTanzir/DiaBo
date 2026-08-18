package com.tanzir.diabo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tanzir.diabo.data.filesystem.FileManager
import com.tanzir.diabo.data.local.DiaBoDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ProjectRepositoryTest {

    private lateinit var db: DiaBoDatabase
    private lateinit var repository: ProjectRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DiaBoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val fileManager = FileManager(context)
        repository = ProjectRepository(fileManager, db.projectDao(), db.projectFileDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `createProject seeds a MainActivity java and activity_main xml file`() = runTest {
        val result = repository.createProject("MyApp")
        assertTrue(result is DiaBoResult.Success)

        val project = (result as DiaBoResult.Success).data
        assertEquals("MyApp", project.name)
        assertEquals("com.diabo.myapp", project.packageName)

        val files = repository.observeProjectFiles(project.id)
        // Just confirm the folder was actually seeded on disk — Flow collection needs a
        // running coroutine, so check the underlying folder directly for this smoke test.
        val projectDir = repository.projectFolder(project)
        assertTrue(java.io.File(projectDir, "src/MainActivity.java").exists())
        assertTrue(java.io.File(projectDir, "res/layout/activity_main.xml").exists())
    }

    @Test
    fun `createProject rejects a blank name`() = runTest {
        val result = repository.createProject("   ")
        assertTrue(result is DiaBoResult.Error)
    }

    @Test
    fun `createProject rejects a duplicate name`() = runTest {
        repository.createProject("DupApp")
        val second = repository.createProject("DupApp")
        assertTrue(second is DiaBoResult.Error)
    }

    @Test
    fun `deleteProject removes both the DB row and the folder on disk`() = runTest {
        val project = (repository.createProject("ToRemove") as DiaBoResult.Success).data
        val projectDir = repository.projectFolder(project)
        assertTrue(projectDir.exists())

        val result = repository.deleteProject(project)
        assertTrue(result is DiaBoResult.Success)
        assertFalse(projectDir.exists())
    }

    @Test
    fun `saveFileContent persists changes and they can be read back`() = runTest {
        val project = (repository.createProject("EditApp") as DiaBoResult.Success).data
        val fileResult = repository.createFile(project, relativeDir = "src", fileName = "Extra.java")
        val file = (fileResult as DiaBoResult.Success).data

        repository.saveFileContent(project, file, "// updated content")
        val readBack = repository.readFileContent(project, file)

        assertEquals("// updated content", (readBack as DiaBoResult.Success).data)
    }

    @Test
    fun `createProject with a template seeds the template's content instead of the default`() = runTest {
        val template = com.tanzir.diabo.templates.TemplateCatalog.all.first { it.id == "login" }
        val project = (repository.createProject("LoginApp", template) as DiaBoResult.Success).data
        val javaFile = java.io.File(repository.projectFolder(project), "src/MainActivity.java")

        assertTrue(javaFile.readText().contains("onLoginClick"))
    }
}
