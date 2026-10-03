package com.qurankareem.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qurankareem.core.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
        )
        if (intent.action !in supported) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(context.applicationContext).settings.first()
                val location = LocationRepository(context.applicationContext).saved()
                PrayerAlarmScheduler(context.applicationContext).reschedule(settings, location)
            } finally {
                pending.finish()
            }
        }
    }
}
