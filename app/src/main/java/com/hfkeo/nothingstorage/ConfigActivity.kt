package com.hfkeo.nothingstorage

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class ConfigActivity : Activity() {
    private val screenBackgroundColor = Color.rgb(16, 17, 19)
    private val surface = Color.rgb(35, 36, 39)
    private val muted = Color.rgb(164, 166, 171)
    private val palette = listOf(
        "Lime" to "#D7F35B", "Red" to "#FF453A", "Blue" to "#55C5FF",
        "Purple" to "#B28CFF", "White" to "#FFFFFF", "Orange" to "#FFB347"
    )
    private val prefs by lazy { getSharedPreferences("widget", MODE_PRIVATE) }
    private lateinit var preview: Preview
    private lateinit var opacityValue: TextView
    private lateinit var colorRow: LinearLayout

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = true
        }
    private fun add(parent: LinearLayout, child: View, height: Int = -2, marginTop: Int = 0) {
        parent.addView(child, LinearLayout.LayoutParams(-1, if (height == -2) -2 else dp(height)).apply {
            topMargin = dp(marginTop)
        })
    }
    private fun section(s: String) = text(s, 11f, muted, true).apply { letterSpacing = .20f }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = screenBackgroundColor
        window.navigationBarColor = screenBackgroundColor
        window.decorView.systemUiVisibility = 0
        val scroll = ScrollView(this).apply { setBackgroundColor(screenBackgroundColor); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(32))
        }
        scroll.addView(root)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = text("●  NOTHING STORAGE", 19f, Color.WHITE, true).apply { letterSpacing = .045f }
        header.addView(heading, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(text("v1.1", 12f, muted), LinearLayout.LayoutParams(-2, dp(48)))
        add(root, header)
        add(root, section("LIVE PREVIEW"), marginTop = 24)
        preview = Preview(this)
        add(root, preview, 168, 12)

        add(root, section("APPEARANCE"), marginTop = 30)
        add(root, text("Accent color", 16f, Color.WHITE, true), marginTop = 18)
        colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        add(root, colorRow, 58, 8)
        renderColors()

        val opacityHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        opacityHeader.addView(text("Background opacity", 16f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(42), 1f))
        opacityValue = text("", 15f, currentAccent(), true)
        opacityHeader.addView(opacityValue)
        add(root, opacityHeader, marginTop = 16)
        val slider = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("opacity", 92).coerceIn(0, 100)
            progressTintList = ColorStateList.valueOf(currentAccent())
            thumbTintList = ColorStateList.valueOf(currentAccent())
            progressBackgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    opacityValue.text = "$value%"
                    if (fromUser) {
                        prefs.edit().putInt("opacity", value).apply()
                        preview.invalidate()
                        StorageWidget.refreshAll(this@ConfigActivity)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        opacityValue.text = "${slider.progress}%"
        add(root, slider, 42)
        val ends = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        ends.addView(text("Transparent", 12f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        ends.addView(text("Opaque", 12f, muted))
        add(root, ends)

        add(root, section("CONTROLS"), marginTop = 32)
        val refresh = text("↻   Refresh storage data", 15f, Color.WHITE, true).apply {
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(surface, 15)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                StorageWidget.refreshAll(this@ConfigActivity)
                preview.invalidate()
                Toast.makeText(this@ConfigActivity, "Storage refreshed", Toast.LENGTH_SHORT).show()
            }
        }
        add(root, refresh, 54, 12)
        add(root, text("Changes apply immediately to your home-screen widget.", 12f, muted), marginTop = 18)
        setContentView(scroll)
    }

    private fun currentAccent() = prefs.getInt("accent", Color.rgb(215, 243, 91))
    private fun renderColors() {
        colorRow.removeAllViews()
        palette.forEach { (_, hex) ->
            val color = Color.parseColor(hex)
            val selected = currentAccent() == color
            val holder = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dp(40).toFloat()
                    setStroke(dp(if (selected) 2 else 1), if (selected) color else Color.rgb(65, 66, 69))
                }
                isClickable = true
                contentDescription = "Set accent color to ${palette.first { it.second == hex }.first}"
                setOnClickListener {
                    prefs.edit().putInt("accent", color).apply()
                    renderColors()
                    preview.invalidate()
                    opacityValue.setTextColor(color)
                    StorageWidget.refreshAll(this@ConfigActivity)
                }
            }
            val swatch = View(this).apply { background = rounded(color, 40) }
            holder.addView(swatch, LinearLayout.LayoutParams(dp(28), dp(28)))
            colorRow.addView(holder, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginEnd = dp(5)
            })
        }
    }

    private inner class Preview(context: Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun label(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
            p.color = color
            p.textSize = size
            p.typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT
            canvas.drawText(value, x, y, p)
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val sx = width / 350f
            val sy = height / 168f
            canvas.save()
            canvas.scale(sx, sy)
            val accent = currentAccent()
            val opacity = prefs.getInt("opacity", 92).coerceIn(0, 100)
            p.color = Color.argb(opacity * 255 / 100, 39, 40, 43)
            canvas.drawRoundRect(RectF(0f, 0f, 350f, 168f), 22f, 22f, p)
            val stats = StatFs(Environment.getDataDirectory().absolutePath)
            val total = stats.totalBytes
            val free = stats.availableBytes
            val used = (total - free).coerceAtLeast(0)
            val pct = if (total > 0) 100.0 * used / total else 0.0
            label(canvas, "●  S T O R A G E", 18f, 28f, 12f, Color.WHITE)
            label(canvas, "↻", 315f, 28f, 19f, Color.LTGRAY)
            val dots = DotMatrix.render(String.format(Locale.US, "%.1f%%", pct), accent)
            p.color = Color.WHITE
            canvas.drawBitmap(dots, null, RectF(16f, 49f, 215f, 103f), p)
            label(canvas, String.format(Locale.US, "%.1f GB", free / 1e9), 243f, 77f, 17f, accent, true)
            label(canvas, "F R E E", 243f, 95f, 9f, muted)
            p.color = Color.rgb(75, 76, 78)
            canvas.drawRoundRect(RectF(18f, 117f, 332f, 124f), 4f, 4f, p)
            p.color = accent
            canvas.drawRoundRect(RectF(18f, 117f, 18f + 314f * (pct / 100.0).toFloat(), 124f), 4f, 4f, p)
            label(canvas, String.format(Locale.US, "%.1f GB used", used / 1e9), 18f, 147f, 10f, muted)
            label(canvas, String.format(Locale.US, "%.1f GB total", total / 1e9), 249f, 147f, 10f, muted)
            canvas.restore()
        }
    }
}
