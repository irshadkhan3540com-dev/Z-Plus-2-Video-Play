package com.zplus.videoplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos_table ORDER BY id DESC")
    fun getAllVideos(): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertVideos(videos: List<VideoEntity>)

    @Query("DELETE FROM videos_table WHERE path = :videoPath")
    fun deleteVideoByPath(videoPath: String)
}

