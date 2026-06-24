package com.zplus.videoplayer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zplus.videoplayer.data.VideoSyncEngine
import com.zplus.videoplayer.ui.VideoAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var txtFolderTitle: TextView
    private lateinit var txtNoVideos: TextView
    private lateinit var syncEngine: VideoSyncEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)

        // Initialize view IDs smoothly
        recyclerView = findViewById(R.id.videoRecyclerView)
        txtFolderTitle = findViewById(R.id.txtFolderTitle)
        txtNoVideos = findViewById(R.id.txtNoVideos)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Setup adapter with empty list initially
        videoAdapter = VideoAdapter { selectedVideo ->
            Toast.makeText(this, "Playing: ${selectedVideo.title}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = videoAdapter
        
        syncEngine = VideoSyncEngine(this)

        // Null-safe Intent unwrap check
        val folderName = intent.getStringExtra("FOLDER_NAME") ?: "Videos"
        txtFolderTitle.text = folderName

        loadFolderVideos(folderName)
    }

    private fun loadFolderVideos(folderName: String) {
        lifecycleScope.launch {
            try {
                // Fetch cached content from background thread
                val allVideos = withContext(Dispatchers.IO) {
                    syncEngine.syncVideosOnAppOpen()
                }

                // Mathematical filtering algorithm run on separated computation thread
                val filteredVideos = withContext(Dispatchers.Default) {
                    allVideos.filter { File(it.path).parentFile?.name == folderName }
                }

                // Clean empty state switching to prevent logic freeze bugs
                if (filteredVideos.isEmpty()) {
                    txtNoVideos.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    txtNoVideos.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    videoAdapter.updateList(filteredVideos)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                txtNoVideos.text = "Failed to load directory safely"
                txtNoVideos.visibility = View.VISIBLE
            }
        }
    }
}

