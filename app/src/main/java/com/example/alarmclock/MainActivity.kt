package com.example.alarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var tvAlarmTime: TextView
    private lateinit var tvAlarmStatus: TextView
    private lateinit var switchAlarm: SwitchMaterial

    // Receives the chosen hour/minute back from TimePickerActivity
    private val timePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val hour   = result.data?.getIntExtra(TimePickerActivity.EXTRA_HOUR, -1)   ?: -1
                val minute = result.data?.getIntExtra(TimePickerActivity.EXTRA_MINUTE, -1) ?: -1
                if (hour >= 0 && minute >= 0) {
                    saveChosenTime(hour, minute)
                    updateAlarmTimeDisplay(hour, minute)
                    // If the switch is already ON, reschedule and refresh the countdown
                    if (switchAlarm.isChecked) {
                        scheduleAlarmAt(hour, minute)
                        applyAlarmActiveStyle(true)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAlarmTime   = findViewById(R.id.tvAlarmTime)
        tvAlarmStatus = findViewById(R.id.tvAlarmStatus)
        switchAlarm   = findViewById(R.id.switchAlarm)

        requestNotificationPermission()
        requestOverlayPermissionIfNeeded()

        // ── Restore persisted state ───────────────────────────
        val prefs     = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val savedHour = prefs.getInt("ALARM_HOUR", -1)
        val savedMin  = prefs.getInt("ALARM_MINUTE", -1)
        val isAlarmOn = prefs.getBoolean("ALARM_ON", false)

        if (savedHour >= 0 && savedMin >= 0) {
            updateAlarmTimeDisplay(savedHour, savedMin)
        }
        // Restore switch silently (without triggering the listener)
        switchAlarm.setOnCheckedChangeListener(null)
        switchAlarm.isChecked = isAlarmOn
        applyAlarmActiveStyle(isAlarmOn)

        // ── Tap time label → open TimePickerActivity ──────────
        tvAlarmTime.setOnClickListener {
            val intent = Intent(this, TimePickerActivity::class.java).apply {
                if (savedHour >= 0) putExtra(TimePickerActivity.EXTRA_HOUR,   savedHour)
                if (savedMin  >= 0) putExtra(TimePickerActivity.EXTRA_MINUTE, savedMin)
            }
            timePickerLauncher.launch(intent)
        }

        // ── Switch: enable / disable alarm ────────────────────
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            val h = prefs.getInt("ALARM_HOUR",   -1)
            val m = prefs.getInt("ALARM_MINUTE", -1)

            if (isChecked) {
                if (h < 0 || m < 0) {
                    switchAlarm.isChecked = false
                    Toast.makeText(this, "Tap the time to set an alarm first", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                if (checkAlarmPermission()) {
                    scheduleAlarmAt(h, m)
                    prefs.edit().putBoolean("ALARM_ON", true).apply()
                    applyAlarmActiveStyle(true)
                    Toast.makeText(this, "Alarm set for ${formatTime(h, m)}", Toast.LENGTH_SHORT).show()
                } else {
                    switchAlarm.isChecked = false
                }
            } else {
                cancelAlarm()
                prefs.edit().putBoolean("ALARM_ON", false).apply()
                applyAlarmActiveStyle(false)
                tvAlarmStatus.text = ""
            }
        }

        // ── Settings and Help buttons ─────────────────────────
        findViewById<android.widget.ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
        findViewById<android.widget.ImageButton>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
    }

    // ── Refresh display when returning from Settings ──────────────────────────
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
        val h = prefs.getInt("ALARM_HOUR",   -1)
        val m = prefs.getInt("ALARM_MINUTE", -1)
        if (h >= 0 && m >= 0) updateAlarmTimeDisplay(h, m)
    }

    // ── Visual state: red when armed ──────────────────────────────────────────

    private fun applyAlarmActiveStyle(active: Boolean) {
        val red      = ContextCompat.getColor(this, R.color.alarm_red)
        val dimWhite = ContextCompat.getColor(this, R.color.text_secondary)

        tvAlarmTime.setTextColor(if (active) red else dimWhite)
        // SwitchMaterial thumb/track colour is driven by colorControlActivated in the theme
        // so we just need to propagate the checked state — already done by the caller.

        if (active) {
            val prefs = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            val h = prefs.getInt("ALARM_HOUR",   -1)
            val m = prefs.getInt("ALARM_MINUTE", -1)
            if (h >= 0 && m >= 0) {
                val timeUntil = minutesUntil(h, m)
                tvAlarmStatus.setTextColor(red)
                tvAlarmStatus.text = formatCountdown(timeUntil)
            }
        }
    }

    private fun updateAlarmTimeDisplay(hour: Int, minute: Int) {
        tvAlarmTime.text = formatTime(hour, minute)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val use24h = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            .getBoolean(ConfigActivity.KEY_24H, false)
        return if (use24h) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h12  = if (hour % 12 == 0) 12 else hour % 12
            val ampm = if (hour < 12) "AM" else "PM"
            String.format("%d:%02d %s", h12, minute, ampm)
        }
    }

    // ── Alarm scheduling ─────────────────────────────────────────────────────

    private fun scheduleAlarmAt(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = Calendar.getInstance().apply {
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.before(now)) calendar.add(Calendar.DATE, 1)

        saveAlarmTimeMillis(calendar.timeInMillis)

        val alarmManager  = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent())
        getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            .edit().remove("ALARM_TIME").apply()
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private fun saveChosenTime(hour: Int, minute: Int) {
        getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE).edit()
            .putInt("ALARM_HOUR",   hour)
            .putInt("ALARM_MINUTE", minute)
            .apply()
    }

    private fun saveAlarmTimeMillis(millis: Long) {
        getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE).edit()
            .putLong("ALARM_TIME", millis)
            .apply()
    }

    // ── Countdown helper ─────────────────────────────────────────────────────

    private fun minutesUntil(hour: Int, minute: Int): Long {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(Calendar.getInstance())) target.add(Calendar.DATE, 1)
        return (target.timeInMillis - System.currentTimeMillis()) / 60_000L
    }

    private fun formatCountdown(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 -> "rings in ${h}h ${m}m"
            else  -> "rings in ${m}m"
        }
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    private fun checkAlarmPermission(): Boolean {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                Toast.makeText(this, "Please allow exact alarms in settings", Toast.LENGTH_LONG).show()
                return false
            }
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun requestOverlayPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
            Toast.makeText(
                this,
                "Enable 'Display over other apps' so the alarm can break through",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
