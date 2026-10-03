package com.qurankareem.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qurankareem.core.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Refresh the rolling schedule even if notification permission is currently disabled.
        val schedulePending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(context.applicationContext).settings.first()
                val location = LocationRepository(context.applicationContext).saved()
                PrayerAlarmScheduler(context.applicationContext).reschedule(settings, location)
            } finally {
                schedulePending.finish()
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME).orEmpty().ifBlank { "الصلاة" }
        val isReminder = intent.getBooleanExtra(EXTRA_IS_REMINDER, false)
        val reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 0)
        val soundUriValue = intent.getStringExtra(EXTRA_SOUND_URI).orEmpty()
        val channelId = createChannel(context, soundUriValue)
        val openApp = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (isReminder) "اقترب موعد صلاة $prayerName" else "حان الآن موعد صلاة $prayerName"
        val body = if (isReminder) "متبقي تقريبًا $reminderMinutes دقيقة" else "تقبّل الله طاعتكم"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    private fun createChannel(context: Context, soundUriValue: String): String {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "prayer_${soundUriValue.hashCode()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, "الأذان وتنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات مواقيت الصلاة والتذكير قبلها"
                enableVibration(true)
                val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                when {
                    soundUriValue == SILENT_SOUND -> setSound(null, null)
                    soundUriValue.isNotBlank() -> setSound(Uri.parse(soundUriValue), audioAttributes)
                    else -> setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
                }
            }
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    companion object {
        const val EXTRA_REQUEST_CODE = "request_code"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_IS_REMINDER = "is_reminder"
        const val EXTRA_REMINDER_MINUTES = "reminder_minutes"
        const val EXTRA_SOUND_URI = "sound_uri"
        private const val SILENT_SOUND = "__silent__"
    }
}
