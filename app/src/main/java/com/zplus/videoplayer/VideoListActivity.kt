package com.zplus.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var searchVideoView: EditText
    private lateinit var btnSort: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var syncEngine: VideoSyncEngine

    private var masterVideoList: List<com.zplus.videoplayer.model.VideoModel> = ArrayList()
    private var currentFolderName: String = "Videos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)

        // Bind all UI Components flawlessly
        recyclerView = findViewById(R.id.videoRecyclerView)
        txtFolderTitle = findViewById(R.id.txtFolderTitle)
        txtNoVideos = findViewById(R.id.txtNoVideos)
        searchVideoView = findViewById(R.id.searchVideoView)
        btnSort = findViewById(R.id.btnSort)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        syncEngine = VideoSyncEngine(this)

        currentFolderName = intent.getStringExtra("FOLDER_NAME") ?: "Videos"
        txtFolderTitle.text = currentFolderName

        // 100% Complete Item Callbacks (Single Click to Play, Long Press to Delete/Share)
        setupAdapterLogic()

        // Real-time Search Processing Engine
        setupSearchEngine()

        // Sorting Option Menu Controller
        btnSort.setOnClickListener { showSortingDialog() }

        // Swipe-to-Refresh Handler
        swipeRefreshLayout.setColorSchemeColors(resources.getColor(android.R.color.holo_green_dark))
        swipeRefreshLayout.setOnRefreshListener {
            loadFolderVideos(currentFolderName)
        }

        // Initial Boot Load
        loadFolderVideos(currentFolderName)
    }

    private fun setupAdapterLogic() {
        // Injected with custom implementation of Adapter supporting double handling
        videoAdapter = VideoAdapter(
            onItemClicked = { selectedVideo ->
                Toast.makeText(this, "Playing: ${selectedVideo.title}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("VIDEO_PATH", selectedVideo.path)
                }
                startActivity(intent)
            },
            onItemLongClicked = { selectedVideo ->
                showActionMenuDialog(selectedVideo)
            }
        )
        recyclerView.adapter = videoAdapter
    }

    private fun loadFolderVideos(folderName: String) {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                // Secure background pipeline data stream
                val allVideos = withContext(Dispatchers.IO) {
                    syncEngine.syncVideosOnAppOpen()
                }

                // Filtering algorithm executed on specialized thread isolation
                masterVideoList = withContext(Dispatchers.Default) {
                    allVideos.filter { File(it.path).parentFile?.name == folderName }
                }

                updateUiState(masterVideoList)

            } catch (e: Exception) {
                e.printStackTrace()
                txtNoVideos.text = "Failed to load directory safely"
                txtNoVideos.visibility = View.VISIBLE
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateUiState(list: List<com.zplus.videoplayer.model.VideoModel>) {
        if (list.isEmpty()) {
            txtNoVideos.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            txtNoVideos.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            videoAdapter.updateList(list)
        }
    }

    private fun setupSearchEngine() {
        searchVideoView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                val filteredList = masterVideoList.filter { it.title.contains(query, ignoreCase = true) }
                updateUiState(filteredList)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showSortingDialog() {
        val sortOptions = arrayOf("Name (A-Z)", "Date (Newest First)", "Size (Largest First)")
        AlertDialog.Builder(this)
            .setTitle("Sort Videos By")
            .setItems(sortOptions) { _, which ->
                lifecycleScope.launch(Dispatchers.Default) {
                    val sortedList = when (which) {
                        0 -> masterVideoList.sortedBy { it.title.lowercase() }
                        1 -> masterVideoList.sortedByDescending { File(it.path).lastModified() }
                        2 -> masterVideoList.sortedByDescending { File(it.path).length() }
                        else -> masterVideoList
                    }
                    withContext(Dispatchers.Main) {
                        masterVideoList = sortedList
                        updateUiState(masterVideoList)
                    }
                }
            }.show()
    }

    private fun showActionMenuDialog(video: com.zplus.videoplayer.model.VideoModel) {
        val menuOptions = arrayOf("Share Video", "Delete Permanently")
        AlertDialog.Builder(this)
            .setTitle(video.title)
            .setItems(menuOptions) { _, which ->
                when (which) {
                    0 -> shareVideoFile(video)
                    1 -> confirmAndDeleteVideo(video)
                }
            }.show()
    }

    private fun shareVideoFile(video: com.zplus.videoplayer.model.VideoModel) {
        try {
            val file = File(video.path)
            val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Video Via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot share this file type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmAndDeleteVideo(video: com.zplus.videoplayer.model.VideoModel) {
        AlertDialog.Builder(this)
            .setTitle("Delete Video?")
            .setMessage("Are you sure you want to delete this video permanently from your device?")
            .setPositiveButton("Delete") { _, _ ->
                val file = File(video.path)
                if (file.exists() && file.delete()) {
                    Toast.makeText(this, "File Deleted Successfully", Toast.LENGTH_SHORT).show()
                    loadFolderVideos(currentFolderName) // Smart refresh list automatically
                } else {
                    Toast.makeText(this, "Permission Denied or File Not Found", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

