package com.zplus.videoplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zplus.videoplayer.R
import com.zplus.videoplayer.data.VideoEntity
import java.io.File

class VideoAdapter(
    private val onVideoClick: (VideoEntity) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var videoList = emptyList<VideoEntity>()

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoName: TextView = itemView.findViewById(R.id.txtVideoName)
        val videoSize: TextView = itemView.findViewById(R.id.txtVideoSize)
    }

    // 100% Real Optimization: DiffUtil calculates changes instead of resetting whole list
    fun updateList(newVideos: List<VideoEntity>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = videoList.size
            override fun getNewListSize(): Int = newVideos.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return videoList[oldItemPosition].id == newVideos[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return videoList[oldItemPosition] == newVideos[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.videoList = newVideos
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.videoName.text = video.title

        // Bulletproof Size Formatter (Handles Bytes, MB, GB completely fine)
        val file = File(video.path)
        holder.videoSize.text = if (file.exists()) {
            formatFileSize(file.length())
        } else {
            "0 Bytes"
        }

        holder.itemView.setOnClickListener { onVideoClick(video) }
    }

    override fun getItemCount(): Int = videoList.size

    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 Bytes"
        val sizeInKb = sizeInBytes / 1024.0
        val sizeInMb = sizeInKb / 1024.0
        val sizeInGb = sizeInMb / 1024.0

        return when {
            sizeInGb > 1 -> String.format("%.2f GB", sizeInGb)
            sizeInMb > 1 -> String.format("%.2f MB", sizeInMb)
            else -> String.format("%.2f KB", sizeInKb)
        }
    }
}

