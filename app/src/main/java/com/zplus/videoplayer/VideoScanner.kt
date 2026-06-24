package com.zplus.videoplayer

import android.content.Context
import android.provider.MediaStore
import java.io.File

// वीडियो की जानकारी स्टोर करने के लिए एक मॉडल डेटा क्लास
data class VideoModel(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long
)

class VideoScanner {

    // यह फंक्शन फोन के स्टोरेज से सारे वीडियो ढूंढ कर लिस्ट बनाएगा
    fun scanLocalVideos(context: Context): ArrayList<VideoModel> {
        val videoList = ArrayList<VideoModel>()
        
        // फोन के मीडिया डेटाबेस से वीडियो का पाथ, नाम, साइज और ड्यूरेशन मांगना
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        // वीडियो को नए से पुराने के क्रम में सॉर्ट करना (Order by Date Added)
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val name = c.getString(nameColumn)
                val path = c.getString(dataColumn)
                val duration = c.getLong(durationColumn)
                val size = c.getLong(sizeColumn)

                // सिर्फ वही फाइलें लें जो असल में मौजूद हैं
                val file = File(path)
                if (file.exists()) {
                    videoList.add(VideoModel(id, name, path, duration, size))
                }
            }
        }
        return videoList
    }
}

