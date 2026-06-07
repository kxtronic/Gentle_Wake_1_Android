package com.kxtronic.gentlewake

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider

class ConfigActivity : AppCompatActivity() {

    companion object {
        const val PREFS              = "AlarmPrefs"
        const val KEY_24H            = "USE_24H"
        const val KEY_DING_INTERVAL  = "DING_INTERVAL_SECS"
        const val DEFAULT_DING_SECS  = 60
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // ── 12 / 24h format ──────────────────────────────────
        val use24h   = prefs.getBoolean(KEY_24H, false)
        val radio12h = findViewById<RadioButton>(R.id.radio12h)
        val radio24h = findViewById<RadioButton>(R.id.radio24h)
        val row12h   = findViewById<LinearLayout>(R.id.row12h)
        val row24h   = findViewById<LinearLayout>(R.id.row24h)

        radio12h.isChecked = !use24h
        radio24h.isChecked =  use24h

        row12h.setOnClickListener {
            radio12h.isChecked = true
            radio24h.isChecked = false
            prefs.edit().putBoolean(KEY_24H, false).apply()
        }
        row24h.setOnClickListener {
            radio24h.isChecked = true
            radio12h.isChecked = false
            prefs.edit().putBoolean(KEY_24H, true).apply()
        }

        // ── Ding interval slider ──────────────────────────────
        val savedSecs = prefs.getInt(KEY_DING_INTERVAL, DEFAULT_DING_SECS)
        val tvDingValue = findViewById<TextView>(R.id.tvDingValue)
        val slider      = findViewById<Slider>(R.id.sliderDingInterval)

        slider.value = savedSecs.toFloat()
        tvDingValue.text = "${savedSecs}s"

        slider.addOnChangeListener { _, value, _ ->
            val secs = value.toInt()
            tvDingValue.text = "${secs}s"
            prefs.edit().putInt(KEY_DING_INTERVAL, secs).apply()
        }

        // ── Back button ───────────────────────────────────────
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
