package com.tanzir.diabo.data.local.dao

import androidx.room.*
import com.tanzir.diabo.data.local.entity.BuildRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildRecordDao {

    @Query("SELECT * FROM build_records WHERE projectId = :projectId ORDER BY triggeredAt DESC")
    fun observeForProject(projectId: String): Flow<List<BuildRecord>>

    @Query("SELECT * FROM build_records ORDER BY triggeredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<BuildRecord>>

    @Query("SELECT * FROM build_records WHERE buildId = :buildId")
    suspend fun getById(buildId: String): BuildRecord?

    @Query("SELECT * FROM build_records WHERE buildId = :buildId")
    fun observeById(buildId: String): Flow<BuildRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: BuildRecord)

    @Query("DELETE FROM build_records WHERE buildId = :buildId")
    suspend fun delete(buildId: String)

    @Query("SELECT * FROM build_records WHERE projectId = :projectId ORDER BY triggeredAt DESC LIMIT 1")
    suspend fun latestForProject(projectId: String): BuildRecord?
}
