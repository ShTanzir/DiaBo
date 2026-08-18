package com.tanzir.diabo.data.local

import androidx.room.TypeConverter
import com.tanzir.diabo.data.local.entity.BuildStatus
import com.tanzir.diabo.data.local.entity.CloudBuildStatus
import com.tanzir.diabo.data.local.entity.FileType

class Converters {
    @TypeConverter
    fun fromBuildStatus(value: BuildStatus): String = value.name

    @TypeConverter
    fun toBuildStatus(value: String): BuildStatus =
        runCatching { BuildStatus.valueOf(value) }.getOrDefault(BuildStatus.NONE)

    @TypeConverter
    fun fromFileType(value: FileType): String = value.name

    @TypeConverter
    fun toFileType(value: String): FileType =
        runCatching { FileType.valueOf(value) }.getOrDefault(FileType.OTHER)

    @TypeConverter
    fun fromCloudBuildStatus(value: CloudBuildStatus): String = value.name

    @TypeConverter
    fun toCloudBuildStatus(value: String): CloudBuildStatus =
        runCatching { CloudBuildStatus.valueOf(value) }.getOrDefault(CloudBuildStatus.FAILED)
}
