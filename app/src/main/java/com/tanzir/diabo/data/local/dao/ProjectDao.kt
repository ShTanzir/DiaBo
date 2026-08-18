package com.tanzir.diabo.data.local.dao

import androidx.room.*
import com.tanzir.diabo.data.local.entity.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun search(query: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): Project?

    @Query("SELECT * FROM projects ORDER BY lastModified DESC LIMIT :limit")
    fun observeRecent(limit: Int = 5): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: Project)

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE folderName = :folderName)")
    suspend fun folderNameExists(folderName: String): Boolean
}
