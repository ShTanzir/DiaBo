package com.tanzir.diabo.data.local.dao

import androidx.room.*
import com.tanzir.diabo.data.local.entity.ProjectFile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectFileDao {

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND isDeleted = 0 ORDER BY isPinned DESC, name ASC")
    fun observeFiles(projectId: String): Flow<List<ProjectFile>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND isDeleted = 1")
    fun observeTrash(projectId: String): Flow<List<ProjectFile>>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getById(id: String): ProjectFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: ProjectFile)

    @Update
    suspend fun update(file: ProjectFile)

    @Delete
    suspend fun hardDelete(file: ProjectFile)

    @Query("UPDATE project_files SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("UPDATE project_files SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query("DELETE FROM project_files WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeExpiredTrash(cutoff: Long)

    @Query("UPDATE project_files SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND name LIKE '%' || :query || '%' AND isDeleted = 0")
    fun searchInProject(projectId: String, query: String): Flow<List<ProjectFile>>
}
