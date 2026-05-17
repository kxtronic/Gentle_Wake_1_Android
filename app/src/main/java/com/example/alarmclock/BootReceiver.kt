package com.example.alarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Validate that this is actually the system boot broadcast signal
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("ALARM_DEBUG", "Phone reboot detected! Rescheduling saved alarm...")

            val prefs = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            val savedTimeMillis = prefs.getLong("ALARM_TIME", 0L)

            // Only reschedule if the saved alarm time is in the future
            if (savedTimeMillis > System.currentTimeMillis()) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    12345, // Must match the request code from MainActivity
                    alarmIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    savedTimeMillis,
                    pendingIntent
                )
                Log.d("ALARM_DEBUG", "Alarm successfully restored after reboot!")
            } else {
                Log.d("ALARM_DEBUG", "Saved alarm time was in the past. No restoration needed.")
            }
        }
    }
}