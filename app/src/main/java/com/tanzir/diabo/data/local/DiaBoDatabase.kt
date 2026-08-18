package com.tanzir.diabo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tanzir.diabo.data.local.dao.BuildRecordDao
import com.tanzir.diabo.data.local.dao.ProjectDao
import com.tanzir.diabo.data.local.dao.ProjectFileDao
import com.tanzir.diabo.data.local.entity.BuildRecord
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.data.local.entity.ProjectFile

/**
 * schemaVersion history:
 *  1 - Phase 1: projects + project_files tables
 *  2 - Phase 3: build_records table (Cloud Build tracking)
 *
 * IMPORTANT: every future schema change MUST ship a Migration (never fallbackToDestructiveMigration
 * in release builds) — losing a user's project index would be a critical bug per the
 * "zero data loss" requirement.
 */
@Database(
    entities = [Project::class, ProjectFile::class, BuildRecord::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DiaBoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun buildRecordDao(): BuildRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `build_records` (
                        `buildId` TEXT NOT NULL PRIMARY KEY,
                        `projectId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `triggeredAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `githubRunId` INTEGER,
                        `githubRunUrl` TEXT,
                        `apkPath` TEXT,
                        `screenshotPath` TEXT,
                        `logSummary` TEXT,
                        `errorMessage` TEXT
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
