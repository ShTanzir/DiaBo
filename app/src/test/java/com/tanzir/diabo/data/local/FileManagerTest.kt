package com.tanzir.diabo.data.filesystem

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class FileManagerTest {

    private lateinit var fileManager: FileManager

    @Before
    fun setUp() {
        fileManager = FileManager(ApplicationProvider.getApplicationContext())
        fileManager.ensureRootStructure()
    }

    @Test
    fun `ensureRootStructure creates all expected folders and is idempotent`() {
        assertTrue(fileManager.rootDir.exists())
        assertTrue(fileManager.projectsDir.exists())
        assertTrue(fileManager.templatesDir.exists())
        assertTrue(fileManager.backupsDir.exists())
        assertTrue(fileManager.buildCacheDir.exists())

        // Calling it again must not throw or duplicate anything.
        val result = fileManager.ensureRootStructure()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `createProjectSkeleton fails clearly if the project already exists`() {
        fileManager.createProjectSkeleton("DuplicateApp")
        val second = fileManager.createProjectSkeleton("DuplicateApp")
        assertTrue(second.isFailure)
    }

    @Test
    fun `safeWrite then safeRead round-trips content exactly`() {
        val projectDir = fileManager.createProjectSkeleton("TestApp").getOrThrow()
        val file = java.io.File(projectDir, "src/MainActivity.java")

        fileManager.safeWrite(file, "hello world")
        val readBack = fileManager.safeRead(file).getOrThrow()

        assertEquals("hello world", readBack)
    }

    @Test
    fun `safeRead on a nonexistent file returns a Failure, never throws uncaught`() {
        val missing = java.io.File(fileManager.projectsDir, "does_not_exist.java")
        val result = fileManager.safeRead(missing)
        assertTrue(result.isFailure)
    }

    @Test
    fun `renameFile refuses to overwrite an existing target`() {
        val projectDir = fileManager.createProjectSkeleton("RenameTest").getOrThrow()
        val a = java.io.File(projectDir, "src/A.java").apply { writeText("a") }
        java.io.File(projectDir, "src/B.java").apply { writeText("b") }

        val result = fileManager.renameFile(a, "B.java")
        assertTrue(result.isFailure)
    }

    @Test
    fun `duplicateFile creates a non-colliding _copy variant`() {
        val projectDir = fileManager.createProjectSkeleton("DupTest").getOrThrow()
        val original = java.io.File(projectDir, "src/Helper.java").apply { writeText("code") }

        val dup1 = fileManager.duplicateFile(original).getOrThrow()
        val dup2 = fileManager.duplicateFile(original).getOrThrow()

        assertEquals("Helper_copy.java", dup1.name)
        assertEquals("Helper_copy1.java", dup2.name)
        assertEquals("code", dup1.readText())
    }

    @Test
    fun `deleteRecursively removes a project folder and all its contents`() {
        val projectDir = fileManager.createProjectSkeleton("ToDelete").getOrThrow()
        java.io.File(projectDir, "src/X.java").writeText("x")

        val result = fileManager.deleteRecursively(projectDir)
        assertTrue(result.isSuccess)
        assertFalse(projectDir.exists())
    }
}
