package com.hfkeo.nothingstorage
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Original 5x7 dot glyphs, not official Nothing fonts. */
object DotMatrix {
    private val glyphs = mapOf(
        '0' to listOf("01110","10001","10011","10101","11001","10001","01110"),
        '1' to listOf("00100","01100","00100","00100","00100","00100","01110"),
        '2' to listOf("01110","10001","00001","00010","00100","01000","11111"),
        '3' to listOf("11110","00001","00001","01110","00001","00001","11110"),
        '4' to listOf("00010","00110","01010","10010","11111","00010","00010"),
        '5' to listOf("11111","10000","10000","11110","00001","00001","11110"),
        '6' to listOf("01111","10000","10000","11110","10001","10001","01110"),
        '7' to listOf("11111","00001","00010","00100","01000","01000","01000"),
        '8' to listOf("01110","10001","10001","01110","10001","10001","01110"),
        '9' to listOf("01110","10001","10001","01111","00001","00001","11110"),
        '.' to listOf("00000","00000","00000","00000","00000","01100","01100"),
        '%' to listOf("11001","11010","00100","00100","01011","10011","00000")
    )
    fun render(value: String, color: Int): Bitmap {
        val step = 8
        val bitmap = Bitmap.createBitmap(((value.length * 6 - 1) * step + 8).coerceAtLeast(1), 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        value.forEachIndexed { index, char ->
            glyphs[char]?.forEachIndexed { y, row ->
                row.forEachIndexed { x, bit ->
                    if (bit == '1') canvas.drawCircle(4f + (index * 6 + x) * step, 4f + y * step, 3f, paint)
                }
            }
        }
        return bitmap
    }
}
