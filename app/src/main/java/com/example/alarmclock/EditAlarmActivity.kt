package com.example.alarmclock

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import com.example.alarmclock.R

class EditAlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_edit_alarm)

            val timePicker = findViewById<TimePicker>(R.id.editTimePicker)
            val btnOk = findViewById<Button>(R.id.btnOk)
            val btnCancel = findViewById<Button>(R.id.btnCancel)

            // Pre-populate picker with previously saved time if available
            val prefs = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            val savedTime = prefs.getLong("ALARM_TIME", 0L)
            if (savedTime != 0L) {
                val cal = Calendar.getInstance().apply { timeInMillis = savedTime }
                timePicker.hour = cal.get(Calendar.HOUR_OF_DAY)
                timePicker.minute = cal.get(Calendar.MINUTE)
            }

            btnCancel.setOnClickListener {
                finish() 
            }

            btnOk.setOnClickListener {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val comparisonNow = Calendar.getInstance().apply {
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (calendar.before(comparisonNow)) {
                    calendar.add(Calendar.DATE, 1)
                }

                prefs.edit().apply {
                    putLong("ALARM_TIME", calendar.timeInMillis)
                    putBoolean("ALARM_ENABLED", true) 
                }.apply()

                finish() 
            }
        } catch (e: Exception) {
            Log.e("ALARM_DEBUG", "CRASH IN EDIT_ALARM_ACTIVITY: ", e)
        }
    }
}