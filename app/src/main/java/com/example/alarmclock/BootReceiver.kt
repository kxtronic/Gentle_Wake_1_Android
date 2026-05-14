package com.example.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            val time = prefs.getLong("ALARM_TIME", 0L)
            if (time > System.currentTimeMillis()) {
                // Reschedule logic here (same as MainActivity)
                // Note: For a complete solution, you'd move the scheduling logic 
                // to a helper function shared by MainActivity and BootReceiver.
            }
        }
    }
}
