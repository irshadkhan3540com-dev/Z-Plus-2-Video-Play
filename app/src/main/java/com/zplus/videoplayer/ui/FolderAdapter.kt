package com.zplus.videoplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zplus.videoplayer.R
import com.zplus.videoplayer.data.VideoEntity
import java.io.File

data class FolderModel(
    val name: String,
    val videos: List<VideoEntity>
)

class FolderAdapter(
    private val folders: List<FolderModel>,
    private val onFolderClick: (FolderModel) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val folderName: TextView = itemView.findViewById(R.id.txtFolderName)
        val videoCount: TextView = itemView.findViewById(R.id.txtVideoCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.folderName.text = folder.name
        
        val countText = "${folder.videos.size} Videos"
        holder.videoCount.text = countText

        holder.itemView.setOnClickListener {
            onFolderClick(folder)
        }
    }

    override fun getItemCount(): Int = folders.size
}

