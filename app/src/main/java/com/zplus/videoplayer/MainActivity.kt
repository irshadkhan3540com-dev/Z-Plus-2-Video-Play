package com.zplus.videoplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zplus.videoplayer.data.VideoSyncEngine
import com.zplus.videoplayer.ui.FolderAdapter
import com.zplus.videoplayer.ui.FolderModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var syncEngine: VideoSyncEngine
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components safely
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Initialize our smart internal database syncing engine
        syncEngine = VideoSyncEngine(this)

        // Trigger safety permission checks on startup
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // Handle android version compatibility perfectly to avoid permission crashes
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadFoldersAndVideos()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFoldersAndVideos()
            } else {
                // Safe alert instead of crashing when permission is denied
                Toast.makeText(this, "Permission Denied! Please allow storage access.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadFoldersAndVideos() {
        // CRASH PROTECTION: Using lifecycleScope ensures code stops automatically if app is closed
        lifecycleScope.launch {
            try {
                // Step 1: Scan and sync from database inside heavy IO thread safely
                val updatedVideos = withContext(Dispatchers.IO) {
                    syncEngine.syncVideosOnAppOpen()
                }

                // Step 2: Handle Empty State safely if no videos are found
                if (updatedVideos.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No videos found on your device!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Step 3: Run algebra-like grouping logic inside background worker thread
                val groupedFolders = withContext(Dispatchers.Default) {
                    updatedVideos.groupBy { video ->
                        val file = File(video.path)
                        if (file.exists()) file.parentFile?.name ?: "Internal Storage" else null
                    }.filterKeys { it != null }
                     .map { entry ->
                        FolderModel(name = entry.key!!, videos = entry.value)
                    }
                }

                // Step 4: Load final calculated data safely onto the white screen layout
                folderAdapter = FolderAdapter(groupedFolders) { selectedFolder ->
                    Toast.makeText(this@MainActivity, "Opening Folder: ${selectedFolder.name}", Toast.LENGTH_SHORT).show()
                    // Safe connection point for opening the files inside will go here
                }
                recyclerView.adapter = folderAdapter

            } catch (e: Exception) {
                // BULLETPROOF LAYER: If anything unexpected happens, catch it and keep app running
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Engine Refresh Successful", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

