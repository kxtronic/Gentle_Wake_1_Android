package com.example.alarmclock

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var dingCount = 1
    private val maxDings = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        // Ensure screen wakes up
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            finish()
        }

        startQuadraticAlarm()
    }

    private fun startQuadraticAlarm() {
        mediaPlayer = MediaPlayer.create(this, R.raw.ding)
        
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

        // We start at 15% volume as a base, and scale the remaining 85% quadratically
        val baseVolume = 0.15f
        val quadraticPart = (progress * progress).toFloat()
        val finalVolume = baseVolume + (0.85f * quadraticPart)

        // Safety check to ensure we never exceed 1.0
        val safeVolume = finalVolume.coerceAtMost(1.0f)

        android.util.Log.d("ALARM_DEBUG", "Ding #$dingCount | Volume: $safeVolume")

        mediaPlayer?.setVolume(safeVolume, safeVolume)
        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
