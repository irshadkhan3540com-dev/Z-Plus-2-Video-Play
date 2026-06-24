package com.zplus.videoplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos_table")
data class VideoEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long
)

