package com.hfkeo.nothingstorage

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.os.Bundle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Environment
import android.os.StatFs
import android.widget.RemoteViews
import java.util.Locale

class StorageWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, id, options)
        update(context, manager, id)
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshAll(context)
    }
    companion object {
        private const val ACTION_REFRESH = "com.hfkeo.nothingstorage.REFRESH"
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, StorageWidget::class.java)).forEach {
                update(context, manager, it, false)
            }
            manager.getAppWidgetIds(ComponentName(context, StorageCompactWidget::class.java)).forEach {
                update(context, manager, it, true)
            }
        }
        private fun format(bytes: Long): String =
            String.format(Locale.getDefault(), "%.1f GB", bytes / 1_000_000_000.0)

        fun update(context: Context, manager: AppWidgetManager, id: Int, compact: Boolean = false) {
            val options = manager.getAppWidgetOptions(id)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, if (compact) 110 else 250)
            val isCompactLayout = minWidth < 220
            val stats = StatFs(Environment.getDataDirectory().absolutePath)
            val total = stats.totalBytes
            val free = stats.availableBytes
            val used = (total - free).coerceAtLeast(0L)
            val fraction = if (total > 0L) used.toDouble() / total else 0.0
            val prefs = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val accent = prefs.getInt("accent", Color.rgb(215, 243, 91))
            val opacity = prefs.getInt("opacity", 92).coerceIn(0, 100)
            val views = RemoteViews(context.packageName, if (isCompactLayout) R.layout.widget_storage_compact else R.layout.widget_storage)
            val bg = Bitmap.createBitmap(500, 230, Bitmap.Config.ARGB_8888)
            Canvas(bg).drawRoundRect(0f, 0f, 500f, 230f, 34f, 34f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(opacity * 255 / 100, 17, 18, 22)
                })
            views.setImageViewBitmap(R.id.background, bg)
            views.setImageViewBitmap(R.id.dot_percentage,
                DotMatrix.render(String.format(Locale.US, "%.1f%%", fraction * 100), accent))
            views.setTextViewText(R.id.free, if (isCompactLayout) "${format(free)} FREE" else format(free))
            views.setTextViewText(R.id.used, "${format(used)} used")
            views.setTextViewText(R.id.total, "${format(total)} total")
            views.setTextColor(R.id.free, accent)
            views.setProgressBar(R.id.bar, 1000, (fraction * 1000).toInt(), false)
            views.setColorStateList(R.id.bar, "setProgressTintList", ColorStateList.valueOf(accent))
            val refresh = PendingIntent.getBroadcast(context, if (compact) 1 else 0,
                Intent(context, if (compact) StorageCompactWidget::class.java else StorageWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.refresh, refresh)
            val settings = PendingIntent.getActivity(context, 0,
                Intent(context, ConfigActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.title, settings)
            manager.updateAppWidget(id, views)
        }
    }
}
