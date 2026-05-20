package com.example.alarmclock

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var dingCount = 1
    private val maxDings = 10
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var alarmExpired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setupLockScreenFlags()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        acquireWakeLock()

        // Restore expired state across rotation / app switching
        val prefs = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        alarmExpired = savedInstanceState?.getBoolean("ALARM_EXPIRED", false)
            ?: prefs.getBoolean("ALARM_EXPIRED", false)

        // Show the alarm time
        val hour   = prefs.getInt("ALARM_HOUR",   -1)
        val minute = prefs.getInt("ALARM_MINUTE", -1)
        if (hour >= 0 && minute >= 0) {
            findViewById<TextView>(R.id.tvAlarmRingTime).text =
                String.format("%02d:%02d", hour, minute)
        }

        // Clear alarm-on flag so the main screen resets its switch after dismiss
        prefs.edit()
            .putBoolean("ALARM_ON", false)
            .remove("ALARM_TIME")
            .apply()

        if (alarmExpired) {
            // Recreated after expiry — just show the expired label, don't replay
            showExpired()
        } else {
            startQuadraticAlarm()
        }

        findViewById<MaterialButton>(R.id.btnDismiss).setOnClickListener {
            stopAlarm()
            // Clear the expired flag when the user consciously dismisses
            getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
                .edit().remove("ALARM_EXPIRED").apply()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("ALARM_EXPIRED", alarmExpired)
    }

    private fun showExpired() {
        findViewById<TextView>(R.id.tvWakeUp).text = "Alarm Expired"
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
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
        wakeLock?.acquire(11 * 60 * 1000L)
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
                handler.postDelayed({ playDing() }, 60_000L)
            } else {
                alarmExpired = true
                // Persist so the state survives app switching
                getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("ALARM_EXPIRED", true).apply()
                stopAlarm()
                runOnUiThread { showExpired() }
            }
        }
        playDing()
    }

    private fun playDing() {
        val progress      = dingCount.toDouble() / maxDings.toDouble()
        val baseVolume    = 0.15f
        val quadraticPart = (progress * progress).toFloat()
        val finalVolume   = (baseVolume + (0.85f * quadraticPart)).coerceAtMost(1.0f)

        mediaPlayer?.setVolume(finalVolume, finalVolume)
        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
