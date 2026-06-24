package com.zplus.videoplayer.data

import android.content.Context
import com.zplus.videoplayer.VideoScanner

class VideoSyncEngine(private val context: Context) {

    private val videoDao = AppDatabase.getDatabase(context).videoDao()
    private val videoScanner = VideoScanner()

    fun syncVideosOnAppOpen(): List<VideoEntity> {
        // 1. Get currently existing videos from phone storage
        val currentStorageVideos = videoScanner.scanLocalVideos(context)
        
        // 2. Get already saved videos from our internal local database server
        val cachedVideos = videoDao.getAllVideos()

        // 3. Smart Logic: Find deleted videos (Present in DB, but missing from storage)
        val currentStoragePaths = currentStorageVideos.map { it.path }.toSet()
        cachedVideos.forEach { cachedVideo ->
            if (!currentStoragePaths.contains(cachedVideo.path)) {
                videoDao.deleteVideoByPath(cachedVideo.path)
            }
        }

        // 4. Smart Logic: Find new videos (Present in storage, but missing from DB)
        val cachedPaths = cachedVideos.map { it.path }.toSet()
        val newVideosToInsert = currentStorageVideos.filter { !cachedPaths.contains(it.path) }.map {
            VideoEntity(it.id, it.title, it.path, it.duration, it.size)
        }

        if (newVideosToInsert.isNotEmpty()) {
            videoDao.insertVideos(newVideosToInsert)
        }

        // 5. Return the final clean and updated list immediately to UI without restarting app
        return videoDao.getAllVideos()
    }
}

