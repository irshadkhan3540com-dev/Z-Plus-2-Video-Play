package com.zplus.videoplayer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Anti-Crash & Premium Step: Make activity completely immersive full-screen
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)

        // Securely fetch video storage path sent from list activity
        val videoPath = intent.getStringExtra("VIDEO_PATH")
        if (videoPath != null) {
            initializePlayer(videoPath)
        } else {
            Toast.makeText(this, "Error: Video path not found!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(videoPath: String) {
        try {
            // Build and attach hyper-fast ExoPlayer engine instance
            player = ExoPlayer.Builder(this).build().also { exoPlayer ->
                playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(videoPath)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true // Autoplay instantly
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to launch playback engine safely", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Smart battery saver: Pause player when app goes to background
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Core Anti-Lag Mechanism: Clean and release hardware codecs immediately
        player?.release()
        player = null
    }
}

