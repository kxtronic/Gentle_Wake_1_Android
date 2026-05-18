package com.example.alarmclock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Dedicated screen for choosing the alarm time.
 * Returns EXTRA_HOUR / EXTRA_MINUTE via setResult → RESULT_OK.
 */
class TimePickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HOUR   = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_picker)

        val timePicker  = findViewById<TimePicker>(R.id.timePicker)
        val btnConfirm  = findViewById<MaterialButton>(R.id.btnConfirm)
        val btnBack     = findViewById<ImageButton>(R.id.btnBack)

        // Pre-fill with the previously saved time (if any)
        val presetHour   = intent.getIntExtra(EXTRA_HOUR,   -1)
        val presetMinute = intent.getIntExtra(EXTRA_MINUTE, -1)
        if (presetHour >= 0 && presetMinute >= 0) {
            timePicker.hour   = presetHour
            timePicker.minute = presetMinute
        }

        btnBack.setOnClickListener { finish() }

        btnConfirm.setOnClickListener {
            val result = Intent().apply {
                putExtra(EXTRA_HOUR,   timePicker.hour)
                putExtra(EXTRA_MINUTE, timePicker.minute)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    // Hardware back → cancel (no result)
    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
