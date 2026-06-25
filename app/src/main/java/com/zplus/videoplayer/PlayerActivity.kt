package com.zplus.videoplayer

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.abs

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private lateinit var audioManager: AudioManager

    // Screen Lock & Features State
    private var isScreenLocked = false
    private var brightnessLevel = -1.0f

    // Gesture Detection
    private lateinit var gestureDetector: GestureDetector
    private var screenWidth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 100% Immersive Full-Screen (Hides Navigation & Status Bar completely)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // Keeps screen awake
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView) // Make sure you are using your custom layout ID if changed
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        screenWidth = resources.displayMetrics.widthPixels

        val videoPath = intent.getStringExtra("VIDEO_PATH")
        if (!videoPath.isNullOrEmpty()) {
            initializePlayer(videoPath)
            setupGestures()
            setupCustomButtons()
        } else {
            Toast.makeText(this, "Error: Corrupted Video File", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializePlayer(videoPath: String) {
        try {
            // Build the ultimate ExoPlayer engine
            player = ExoPlayer.Builder(this)
                .setSeekForwardIncrementMs(10000) // 10 seconds double tap forward
                .setSeekBackIncrementMs(10000)
                .build().also { exoPlayer ->
                    playerView.player = exoPlayer
                    val mediaItem = MediaItem.fromUri(videoPath)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true // Autoplay instantly

                    // Attach 300% Audio Booster (Loudness Enhancer) securely
                    attachAudioBooster(exoPlayer.audioSessionId)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to launch playback engine securely", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attachAudioBooster(sessionId: Int) {
        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId)
            loudnessEnhancer?.enabled = true
            loudnessEnhancer?.setTargetGain(1500) // Auto boost by default slightly for better clarity
        } catch (e: Exception) {
            e.printStackTrace() // Prevents crash if device hardware doesn't support audio FX
        }
    }

    // --- VIP FEATURE: MX PLAYER STYLE SWIPE GESTURES ---
    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isScreenLocked || e1 == null) return false

                val deltaY = e1.y - e2.y
                val deltaX = e1.x - e2.x

                // Ignore accidental horizontal swipes
                if (abs(deltaX) > abs(deltaY)) return false 

                if (e1.x < screenWidth / 2) {
                    // LEFT SIDE: Brightness Control
                    val increase = deltaY > 0
                    adjustBrightness(increase)
                } else {
                    // RIGHT SIDE: Volume Control
                    val increase = deltaY > 0
                    adjustVolume(increase)
                }
                return true
            }
        })

        // Attach gesture to player screen
        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun adjustVolume(increase: Boolean) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        
        var newVol = currentVol
        if (increase && currentVol < maxVol) newVol++
        else if (!increase && currentVol > 0) newVol--

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        // Toast.makeText(this, "Volume: ${(newVol * 100) / maxVol}%", Toast.LENGTH_SHORT).show()
    }

    private fun adjustBrightness(increase: Boolean) {
        val lp = window.attributes
        if (brightnessLevel == -1.0f) brightnessLevel = lp.screenBrightness
        
        if (increase && brightnessLevel < 1.0f) brightnessLevel += 0.05f
        else if (!increase && brightnessLevel > 0.01f) brightnessLevel -= 0.05f

        lp.screenBrightness = brightnessLevel.coerceIn(0.01f, 1.0f)
        window.attributes = lp
    }

    // --- SCREEN LOCK & ADVANCED MENU ---
    private fun setupCustomButtons() {
        // (Note: Ensure these IDs exist in your custom_player_controls.xml)
        val btnLock = playerView.findViewById<ImageButton>(R.id.btnLock)
        val btnMenu = playerView.findViewById<ImageButton>(R.id.btnMenu)

        btnLock?.setOnClickListener {
            isScreenLocked = !isScreenLocked
            if (isScreenLocked) {
                playerView.useController = false // Hide all controls completely
                Toast.makeText(this, "Screen Locked (Tap to Unlock)", Toast.LENGTH_SHORT).show()
                btnLock.visibility = View.VISIBLE // Keep only lock button visible
            } else {
                playerView.useController = true
                Toast.makeText(this, "Screen Unlocked", Toast.LENGTH_SHORT).show()
            }
        }

        btnMenu?.setOnClickListener {
            showVipMenu()
        }
    }

    private fun showVipMenu() {
        val options = arrayOf("Super Audio Boost (300%)", "Normal Audio", "Audio Tracks (Dual Audio)")
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Video Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { loudnessEnhancer?.setTargetGain(3000); Toast.makeText(this, "300% Boost Activated!", Toast.LENGTH_SHORT).show() }
                    1 -> { loudnessEnhancer?.setTargetGain(0); Toast.makeText(this, "Normal Audio Resumed", Toast.LENGTH_SHORT).show() }
                    2 -> { Toast.makeText(this, "Select Audio Track from Bottom Setting icon", Toast.LENGTH_LONG).show() }
                }
            }.show()
    }

    override fun onPause() {
        super.onPause()
        player?.pause() // Smart battery & data saver
    }

    override fun onResume() {
        super.onResume()
        if (player?.isPlaying == false) player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Core Anti-Lag Mechanism: Clean and release hardware codecs immediately
        loudnessEnhancer?.release()
        player?.release()
        player = null
    }
}

