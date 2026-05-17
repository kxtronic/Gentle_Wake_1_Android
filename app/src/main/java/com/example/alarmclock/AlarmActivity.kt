package com.example.alarmclock

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var dingCount = 1
    private val maxDings = 10
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. MUST register lock screen breakthroughs BEFORE super.onCreate or setContentView
        setupLockScreenFlags()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // 2. Force hardware CPU to stay awake for audio calculations
        acquireWakeLock()

        // 3. Bind UI interaction elements
        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }

        // 4. Fire the audio mechanism
        startQuadraticAlarm()
    }

    private fun setupLockScreenFlags() {
        // Handle O_MR1 (Android 8.1 / API 27) and newer modern background window states
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        // Reinforce and force window layer behaviors for persistent custom skins
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "QuadraticAlarm:WakeLockTag"
        )
        wakeLock?.acquire(60 * 1000L) // Safety cap at 1 minute
    }

    private fun startQuadraticAlarm() {
        mediaPlayer = MediaPlayer.create(this, R.raw.ding) ?: return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        mediaPlayer?.setAudioAttributes(attributes)

        mediaPlayer?.setOnCompletionListener {
            if (dingCount < maxDings) {
                dingCount++
                playDing()
            } else {
                stopAlarm()
            }
        }
        playDing()
    }

    private fun playDing() {
        val progress = dingCount.toDouble() / maxDings.toDouble()
        val baseVolume = 0.15f
        val quadraticPart = (progress * progress).toFloat()
        val finalVolume = (baseVolume + (0.85f * quadraticPart)).coerceAtMost(1.0f)

        mediaPlayer?.setVolume(finalVolume, finalVolume)
        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}