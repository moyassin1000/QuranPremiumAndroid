package com.qurankareem.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qurankareem.core.prayer.PrayerCalculator
import com.qurankareem.core.settings.AppSettings
import com.qurankareem.core.settings.PrayerKey
import java.time.LocalDate

class PrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("prayer_alarm_schedule", Context.MODE_PRIVATE)

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun reschedule(settings: AppSettings, location: AppLocation) {
        cancelAllTracked()
        if (!settings.anyPrayerAlertEnabled) return

        val now = System.currentTimeMillis()
        val requestCodes = mutableSetOf<String>()
        for (dayOffset in 0..2L) {
            val date = LocalDate.now().plusDays(dayOffset)
            val day = PrayerCalculator.calculate(location.latitude, location.longitude, date)
            val prayerMoments = listOf(
                PrayerKey.FAJR to day.fajr,
                PrayerKey.DHUHR to day.dhuhr,
                PrayerKey.ASR to day.asr,
                PrayerKey.MAGHRIB to day.maghrib,
                PrayerKey.ISHA to day.isha,
            )
            prayerMoments.forEachIndexed { index, (key, moment) ->
                val config = settings.prayer(key)
                if (!config.enabled) return@forEachIndexed
                val baseCode = (date.toEpochDay() % 100_000L).toInt() * 20 + index * 2
                if (moment.epochMillis > now + 2_000) {
                    scheduleOne(
                        requestCode = baseCode,
                        triggerAtMillis = moment.epochMillis,
                        prayerName = key.arabicName,
                        isReminder = false,
                        soundUri = settings.adhanSoundUri,
                        precise = settings.precisePrayerAlerts,
                    )
                    requestCodes += baseCode.toString()
                }
                if (config.reminderMinutes > 0) {
                    val reminderAt = moment.epochMillis - config.reminderMinutes * 60_000L
                    if (reminderAt > now + 2_000) {
                        val reminderCode = baseCode + 1
                        scheduleOne(
                            requestCode = reminderCode,
                            triggerAtMillis = reminderAt,
                            prayerName = key.arabicName,
                            isReminder = true,
                            reminderMinutes = config.reminderMinutes,
                            soundUri = settings.adhanSoundUri,
                            precise = settings.precisePrayerAlerts,
                        )
                        requestCodes += reminderCode.toString()
                    }
                }
            }
        }
        prefs.edit().putStringSet(KEY_REQUEST_CODES, requestCodes).apply()
    }

    fun cancelAllTracked() {
        prefs.getStringSet(KEY_REQUEST_CODES, emptySet()).orEmpty().forEach { value ->
            value.toIntOrNull()?.let { code ->
                pendingIntent(code, create = false)?.let(alarmManager::cancel)
            }
        }
        prefs.edit().remove(KEY_REQUEST_CODES).apply()
    }

    private fun scheduleOne(
        requestCode: Int,
        triggerAtMillis: Long,
        prayerName: String,
        isReminder: Boolean,
        reminderMinutes: Int = 0,
        soundUri: String,
        precise: Boolean,
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerAlarmReceiver.EXTRA_REQUEST_CODE, requestCode)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_IS_REMINDER, isReminder)
            putExtra(PrayerAlarmReceiver.EXTRA_REMINDER_MINUTES, reminderMinutes)
            putExtra(PrayerAlarmReceiver.EXTRA_SOUND_URI, soundUri)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (precise && canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(requestCode: Int, create: Boolean): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, PrayerAlarmReceiver::class.java),
        (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val KEY_REQUEST_CODES = "request_codes"
    }
}
