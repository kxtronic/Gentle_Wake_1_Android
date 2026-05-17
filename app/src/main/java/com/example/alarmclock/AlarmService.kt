package com.example.alarmclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var dingCount = 1
    private val maxDings = 10

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "quadratic_alarm_channel_final"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Active Alarm System", NotificationManager.IMPORTANCE_MAX
            ).apply {
                setSound(null, null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 1. Build a clean utility notification to anchor the Foreground Service
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alarm System Running")
            .setContentText("Processing wake event...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1002, notification)
        }

        // 2. CRITICAL BYPASS: Forcefully start the full-screen UI using the overlay capability
        try {
            val uiIntent = Intent(this, AlarmActivity::class.java).apply {
                // These specific flags force the window layout engine to generate a fresh window allocation
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                         Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                         Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(uiIntent)
            Log.d("ALARM_DEBUG", "AlarmActivity directly invoked via service context.")
        } catch (e: Exception) {
            Log.e("ALARM_DEBUG", "Direct launch failed, fallback processing required", e)
        }

        startQuadraticMedia()

        return START_STICKY
    }

    private fun startQuadraticMedia() {
        if (mediaPlayer != null) return
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
                stopSelf()
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}