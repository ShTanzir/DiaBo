package com.tanzir.diabo.di

import android.content.Context
import androidx.room.Room
import com.tanzir.diabo.data.local.DiaBoDatabase
import com.tanzir.diabo.data.local.dao.BuildRecordDao
import com.tanzir.diabo.data.local.dao.ProjectDao
import com.tanzir.diabo.data.local.dao.ProjectFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiaBoDatabase =
        Room.databaseBuilder(context, DiaBoDatabase::class.java, "diabo.db")
            // No destructive fallback: schema changes must go through explicit Migrations.
            .addMigrations(DiaBoDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideProjectDao(db: DiaBoDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideProjectFileDao(db: DiaBoDatabase): ProjectFileDao = db.projectFileDao()

    @Provides
    fun provideBuildRecordDao(db: DiaBoDatabase): BuildRecordDao = db.buildRecordDao()
}
