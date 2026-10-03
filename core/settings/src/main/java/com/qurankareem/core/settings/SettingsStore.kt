package com.qurankareem.core.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class PrayerKey(val arabicName: String) {
    FAJR("الفجر"),
    DHUHR("الظهر"),
    ASR("العصر"),
    MAGHRIB("المغرب"),
    ISHA("العشاء"),
}

data class PrayerAlertSetting(
    val enabled: Boolean = false,
    val reminderMinutes: Int = 10,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val wifiOnlyDownloads: Boolean = false,
    val defaultReciterId: Int = 0,
    val defaultMoshafId: Int = 0,
    val playbackSpeed: Float = 1f,
    val repeatOne: Boolean = false,
    val precisePrayerAlerts: Boolean = false,
    val adhanSoundUri: String = "",
    val adhanSoundLabel: String = "صوت النظام",
    val fajr: PrayerAlertSetting = PrayerAlertSetting(),
    val dhuhr: PrayerAlertSetting = PrayerAlertSetting(),
    val asr: PrayerAlertSetting = PrayerAlertSetting(),
    val maghrib: PrayerAlertSetting = PrayerAlertSetting(),
    val isha: PrayerAlertSetting = PrayerAlertSetting(),
) {
    fun prayer(key: PrayerKey): PrayerAlertSetting = when (key) {
        PrayerKey.FAJR -> fajr
        PrayerKey.DHUHR -> dhuhr
        PrayerKey.ASR -> asr
        PrayerKey.MAGHRIB -> maghrib
        PrayerKey.ISHA -> isha
    }

    val anyPrayerAlertEnabled: Boolean
        get() = PrayerKey.entries.any { prayer(it).enabled }
}

class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val wifiOnlyKey = booleanPreferencesKey("wifi_only_downloads")
    private val reciterKey = intPreferencesKey("default_reciter_id")
    private val moshafKey = intPreferencesKey("default_moshaf_id")
    private val playbackSpeedKey = floatPreferencesKey("playback_speed")
    private val repeatOneKey = booleanPreferencesKey("repeat_one")
    private val precisePrayerAlertsKey = booleanPreferencesKey("precise_prayer_alerts")
    private val adhanSoundUriKey = stringPreferencesKey("adhan_sound_uri")
    private val adhanSoundLabelKey = stringPreferencesKey("adhan_sound_label")

    val settings: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            wifiOnlyDownloads = prefs[wifiOnlyKey] ?: false,
            defaultReciterId = prefs[reciterKey] ?: 0,
            defaultMoshafId = prefs[moshafKey] ?: 0,
            playbackSpeed = (prefs[playbackSpeedKey] ?: 1f).coerceIn(0.75f, 2f),
            repeatOne = prefs[repeatOneKey] ?: false,
            precisePrayerAlerts = prefs[precisePrayerAlertsKey] ?: false,
            adhanSoundUri = prefs[adhanSoundUriKey] ?: "",
            adhanSoundLabel = prefs[adhanSoundLabelKey] ?: "صوت النظام",
            fajr = prayerSetting(prefs, PrayerKey.FAJR),
            dhuhr = prayerSetting(prefs, PrayerKey.DHUHR),
            asr = prayerSetting(prefs, PrayerKey.ASR),
            maghrib = prayerSetting(prefs, PrayerKey.MAGHRIB),
            isha = prayerSetting(prefs, PrayerKey.ISHA),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[themeKey] = mode.name }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) = edit { it[wifiOnlyKey] = enabled }

    suspend fun setDefaultReciter(reciterId: Int, moshafId: Int) = edit {
        it[reciterKey] = reciterId
        it[moshafKey] = moshafId
    }

    suspend fun setPlaybackSpeed(speed: Float) = edit { it[playbackSpeedKey] = speed.coerceIn(0.75f, 2f) }

    suspend fun setRepeatOne(enabled: Boolean) = edit { it[repeatOneKey] = enabled }

    suspend fun setPrecisePrayerAlerts(enabled: Boolean) = edit { it[precisePrayerAlertsKey] = enabled }

    suspend fun setAdhanSound(uri: String, label: String) = edit {
        it[adhanSoundUriKey] = uri
        it[adhanSoundLabelKey] = label.ifBlank { "صوت النظام" }
    }

    suspend fun setPrayerEnabled(key: PrayerKey, enabled: Boolean) = edit {
        it[prayerEnabledKey(key)] = enabled
    }

    suspend fun setPrayerReminderMinutes(key: PrayerKey, minutes: Int) = edit {
        it[prayerReminderKey(key)] = minutes.coerceIn(0, 60)
    }


    suspend fun replaceAll(settings: AppSettings) = edit { prefs ->
        prefs[themeKey] = settings.themeMode.name
        prefs[wifiOnlyKey] = settings.wifiOnlyDownloads
        prefs[reciterKey] = settings.defaultReciterId
        prefs[moshafKey] = settings.defaultMoshafId
        prefs[playbackSpeedKey] = settings.playbackSpeed.coerceIn(0.75f, 2f)
        prefs[repeatOneKey] = settings.repeatOne
        prefs[precisePrayerAlertsKey] = settings.precisePrayerAlerts
        prefs[adhanSoundUriKey] = settings.adhanSoundUri
        prefs[adhanSoundLabelKey] = settings.adhanSoundLabel.ifBlank { "صوت النظام" }
        PrayerKey.entries.forEach { key ->
            prefs[prayerEnabledKey(key)] = settings.prayer(key).enabled
            prefs[prayerReminderKey(key)] = settings.prayer(key).reminderMinutes.coerceIn(0, 60)
        }
    }

    private suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.appSettingsDataStore.edit { prefs -> block(prefs) }
    }

    private fun prayerSetting(prefs: Preferences, key: PrayerKey): PrayerAlertSetting = PrayerAlertSetting(
        enabled = prefs[prayerEnabledKey(key)] ?: false,
        reminderMinutes = (prefs[prayerReminderKey(key)] ?: 10).coerceIn(0, 60),
    )

    private fun prayerEnabledKey(key: PrayerKey) = booleanPreferencesKey("prayer_${key.name.lowercase()}_enabled")
    private fun prayerReminderKey(key: PrayerKey) = intPreferencesKey("prayer_${key.name.lowercase()}_reminder")
}
