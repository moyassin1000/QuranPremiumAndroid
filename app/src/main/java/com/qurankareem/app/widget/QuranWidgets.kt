package com.qurankareem.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.qurankareem.app.AppLocation
import com.qurankareem.app.MainActivity
import com.qurankareem.app.R
import com.qurankareem.app.ReadingState
import com.qurankareem.core.practice.PracticeState
import com.qurankareem.core.prayer.PrayerCalculator
import java.time.LocalDate

private const val PREFS = "quran_widget_snapshot"
private const val EXTRA_DESTINATION = "destination"

object WidgetSnapshotPublisher {
    fun publish(context: Context, reading: ReadingState, practice: PracticeState, location: AppLocation) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("last_page", reading.lastPage)
            .putBoolean("khatma_active", practice.khatma.active)
            .putInt("wird_done", practice.khatma.todayCount)
            .putInt("wird_goal", practice.khatma.dailyGoal)
            .putLong("lat_bits", location.latitude.toBits())
            .putLong("lon_bits", location.longitude.toBits())
            .putString("location_label", location.label)
            .apply()
        updateAll(context)
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            PrayerWidgetProvider::class.java,
            ReadingWidgetProvider::class.java,
            WirdWidgetProvider::class.java,
        ).forEach { clazz ->
            val component = ComponentName(context, clazz)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(context, clazz).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                })
            }
        }
    }
}

private fun launchIntent(context: Context, requestCode: Int, destination: String): PendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    Intent(context, MainActivity::class.java)
        .putExtra(EXTRA_DESTINATION, destination)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

class PrayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = Double.fromBits(prefs.getLong("lat_bits", 30.0444.toBits()))
        val lon = Double.fromBits(prefs.getLong("lon_bits", 31.2357.toBits()))
        val label = prefs.getString("location_label", "القاهرة") ?: "القاهرة"
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val day = PrayerCalculator.calculate(lat, lon, today)
        val next = day.all.firstOrNull { it.name != "الشروق" && it.epochMillis > now }
            ?: PrayerCalculator.calculate(lat, lon, today.plusDays(1)).fajr
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_prayer).apply {
                setTextViewText(R.id.widget_title, next.name)
                setTextViewText(R.id.widget_value, PrayerCalculator.format(next.epochMillis))
                setTextViewText(R.id.widget_subtitle, label)
                setOnClickPendingIntent(R.id.widget_root, launchIntent(context, 101, "prayer"))
            }
            manager.updateAppWidget(id, views)
        }
    }
}

class ReadingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val page = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("last_page", 1).coerceIn(1, 604)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_reading).apply {
                setTextViewText(R.id.widget_value, "آخر موضع: الصفحة $page")
                setOnClickPendingIntent(R.id.widget_root, launchIntent(context, 102, "reader/page/$page"))
            }
            manager.updateAppWidget(id, views)
        }
    }
}

class WirdWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val active = prefs.getBoolean("khatma_active", false)
        val done = prefs.getInt("wird_done", 0)
        val goal = prefs.getInt("wird_goal", 0)
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_wird).apply {
                setTextViewText(R.id.widget_title, if (active) "$done / $goal صفحة" else "لا توجد ختمة نشطة")
                setTextViewText(R.id.widget_value, if (active) "متبقي ${(goal - done).coerceAtLeast(0)} صفحة اليوم" else "ابدأ ختمة من داخل التطبيق")
                setOnClickPendingIntent(R.id.widget_root, launchIntent(context, 103, "khatma"))
            }
            manager.updateAppWidget(id, views)
        }
    }
}
