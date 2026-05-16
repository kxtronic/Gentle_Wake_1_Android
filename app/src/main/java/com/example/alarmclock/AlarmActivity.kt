package com.example.alarmclock

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var dingCount = 1
    private val maxDings = 10
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupLockScreenFlags()
        setContentView(R.layout.activity_alarm)
        acquireWakeLock()

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }

        startQuadraticAlarm()
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "QuadraticAlarm:WakeLockTag"
        )
        wakeLock?.acquire(60 * 1000L)
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