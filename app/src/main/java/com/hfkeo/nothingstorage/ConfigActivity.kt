package com.hfkeo.nothingstorage

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class ConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.rgb(17,18,22))
        val prefs = getSharedPreferences("widget", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36,64,36,36)
        }
        fun label(value: String, size: Float = 15f) = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 12)
        }
        root.addView(label("●  NOTHING STORAGE", 24f))
        root.addView(label("ACCENT COLOR"))
        listOf(
            "Lime" to "#D7F35B", "Red" to "#FF453A", "Blue" to "#55C5FF",
            "Purple" to "#B28CFF", "White" to "#FFFFFF", "Orange" to "#FFB347"
        ).forEach { (name, hex) ->
            root.addView(Button(this).apply {
                text = name
                isAllCaps = false
                setOnClickListener {
                    prefs.edit().putInt("accent", Color.parseColor(hex)).apply()
                    StorageWidget.refreshAll(this@ConfigActivity)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val alphaLabel = label("BACKGROUND OPACITY: ${prefs.getInt("opacity", 92)}%")
        root.addView(alphaLabel)
        root.addView(SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("opacity", 92)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    alphaLabel.text = "BACKGROUND OPACITY: $progress%"
                    if (fromUser) {
                        prefs.edit().putInt("opacity", progress).apply()
                        StorageWidget.refreshAll(this@ConfigActivity)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
        root.addView(label("0% transparent • 100% opaque", 12f))
        root.addView(label("Storage readings refresh periodically or when you tap ↻. Tap STORAGE on the widget to reopen settings.", 13f))
        setContentView(root)
    }
}
