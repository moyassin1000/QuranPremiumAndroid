package com.qurankareem.app

import com.qurankareem.core.practice.HifzItem
import com.qurankareem.core.practice.HifzStage
import com.qurankareem.core.practice.KhatmaProgress
import com.qurankareem.core.practice.PracticeState
import com.qurankareem.core.practice.PracticeStore
import com.qurankareem.core.quran.QuranMetadata
import com.qurankareem.core.settings.AppSettings
import com.qurankareem.core.settings.PrayerAlertSetting
import com.qurankareem.core.settings.PrayerKey
import com.qurankareem.core.settings.SettingsStore
import com.qurankareem.core.settings.ThemeMode
import com.qurankareem.core.stats.StatsBackupData
import com.qurankareem.core.stats.StatsStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

object UserBackupManager {
    private const val FORMAT = "quran-premium-backup"
    private const val SCHEMA_VERSION = 1

    suspend fun exportJson(
        readingStore: ReadingStore,
        settingsStore: SettingsStore,
        practiceStore: PracticeStore,
        statsStore: StatsStore,
        locationRepository: LocationRepository,
    ): String {
        val reading = readingStore.state.first()
        val settings = settingsStore.settings.first()
        val practice = practiceStore.state.first()
        val stats = statsStore.exportBackup()
        val location = locationRepository.saved()

        return JSONObject().apply {
            put("format", FORMAT)
            put("schemaVersion", SCHEMA_VERSION)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("exportedAt", Instant.now().toString())
            put("exportedDate", LocalDate.now().toString())
            put("reading", reading.toJson())
            put("settings", settings.toJson())
            put("practice", practice.toJson())
            put("stats", stats.toJson())
            put("location", location.toJson())
        }.toString(2)
    }

    suspend fun restoreJson(
        rawJson: String,
        readingStore: ReadingStore,
        settingsStore: SettingsStore,
        practiceStore: PracticeStore,
        statsStore: StatsStore,
        locationRepository: LocationRepository,
    ): AppLocation {
        val root = JSONObject(rawJson)
        require(root.optString("format") == FORMAT) { "صيغة النسخة الاحتياطية غير معروفة" }
        val schema = root.optInt("schemaVersion", -1)
        require(schema in 1..SCHEMA_VERSION) { "إصدار النسخة الاحتياطية غير مدعوم" }

        // Parse and validate everything before modifying any local store.
        val reading = parseReading(root.getJSONObject("reading"))
        val settings = parseSettings(root.getJSONObject("settings"))
        val practice = parsePractice(root.getJSONObject("practice"))
        val stats = parseStats(root.getJSONObject("stats"))
        val location = parseLocation(root.getJSONObject("location"))
        val restoreDailyState = root.optString("exportedDate") == LocalDate.now().toString()

        readingStore.replaceState(reading)
        settingsStore.replaceAll(settings)
        practiceStore.replaceState(practice, restoreDailyState = restoreDailyState)
        statsStore.replaceBackup(stats)
        locationRepository.save(location)
        return location
    }

    private fun ReadingState.toJson() = JSONObject().apply {
        put("lastPage", lastPage)
        put("bookmarkedPages", bookmarkedPages.toJsonArray())
        put("favoritePages", favoritePages.toJsonArray())
        put("bookmarkedAyahs", bookmarkedAyahs.toJsonArray())
        put("favoriteAyahs", favoriteAyahs.toJsonArray())
    }

    private fun AppSettings.toJson() = JSONObject().apply {
        put("themeMode", themeMode.name)
        put("wifiOnlyDownloads", wifiOnlyDownloads)
        put("defaultReciterId", defaultReciterId)
        put("defaultMoshafId", defaultMoshafId)
        put("playbackSpeed", playbackSpeed.toDouble())
        put("repeatOne", repeatOne)
        put("precisePrayerAlerts", precisePrayerAlerts)
        put("adhanSoundUri", adhanSoundUri)
        put("adhanSoundLabel", adhanSoundLabel)
        put("prayers", JSONObject().apply {
            PrayerKey.entries.forEach { key ->
                val value = prayer(key)
                put(key.name, JSONObject().apply {
                    put("enabled", value.enabled)
                    put("reminderMinutes", value.reminderMinutes)
                })
            }
        })
    }

    private fun PracticeState.toJson() = JSONObject().apply {
        put("hifzItems", JSONArray().apply {
            hifzItems.forEach { item ->
                put(JSONObject().apply {
                    put("surah", item.surah)
                    put("fromAyah", item.fromAyah)
                    put("toAyah", item.toAyah)
                    put("ayahRepeat", item.ayahRepeat)
                    put("rangeRepeat", item.rangeRepeat)
                    put("stage", item.stage.name)
                    put("reviewLevel", item.reviewLevel)
                    put("nextReviewEpochDay", item.nextReviewEpochDay)
                })
            }
        })
        put("khatma", JSONObject().apply {
            put("active", khatma.active)
            put("goalDays", khatma.goalDays)
            put("startedEpochDay", khatma.startedEpochDay)
            put("completedPages", khatma.completedPages.toJsonArray())
            put("todayPages", khatma.todayPages.toJsonArray())
        })
        put("adhkarCounts", JSONObject().apply {
            adhkarCounts.forEach { (id, count) -> put(id, count) }
        })
    }

    private fun StatsBackupData.toJson() = JSONObject().apply {
        put("totalPageViews", totalPageViews)
        put("uniquePages", uniquePages.toJsonArray())
        put("audioSeconds", audioSeconds)
        put("readingDays", readingDays.toJsonArray())
        put("dailyPageCounts", JSONObject().apply {
            dailyPageCounts.forEach { (day, count) -> put(day.toString(), count) }
        })
    }

    private fun AppLocation.toJson() = JSONObject().apply {
        put("latitude", latitude)
        put("longitude", longitude)
        put("label", label)
    }

    private fun parseReading(json: JSONObject) = ReadingState(
        lastPage = json.optInt("lastPage", 1),
        bookmarkedPages = json.optJSONArray("bookmarkedPages").toIntSet(),
        favoritePages = json.optJSONArray("favoritePages").toIntSet(),
        bookmarkedAyahs = json.optJSONArray("bookmarkedAyahs").toStringSet(),
        favoriteAyahs = json.optJSONArray("favoriteAyahs").toStringSet(),
    )

    private fun parseSettings(json: JSONObject): AppSettings {
        val prayers = json.optJSONObject("prayers") ?: JSONObject()
        fun prayer(key: PrayerKey): PrayerAlertSetting {
            val value = prayers.optJSONObject(key.name) ?: JSONObject()
            return PrayerAlertSetting(
                enabled = value.optBoolean("enabled", false),
                reminderMinutes = value.optInt("reminderMinutes", 10).coerceIn(0, 60),
            )
        }
        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(json.optString("themeMode", ThemeMode.SYSTEM.name)) }.getOrDefault(ThemeMode.SYSTEM),
            wifiOnlyDownloads = json.optBoolean("wifiOnlyDownloads", false),
            defaultReciterId = json.optInt("defaultReciterId", 0),
            defaultMoshafId = json.optInt("defaultMoshafId", 0),
            playbackSpeed = json.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.75f, 2f),
            repeatOne = json.optBoolean("repeatOne", false),
            precisePrayerAlerts = json.optBoolean("precisePrayerAlerts", false),
            adhanSoundUri = json.optString("adhanSoundUri", ""),
            adhanSoundLabel = json.optString("adhanSoundLabel", "صوت النظام").ifBlank { "صوت النظام" },
            fajr = prayer(PrayerKey.FAJR),
            dhuhr = prayer(PrayerKey.DHUHR),
            asr = prayer(PrayerKey.ASR),
            maghrib = prayer(PrayerKey.MAGHRIB),
            isha = prayer(PrayerKey.ISHA),
        )
    }

    private fun parsePractice(json: JSONObject): PracticeState {
        val items = buildList {
            val array = json.optJSONArray("hifzItems") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val stage = runCatching { HifzStage.valueOf(item.optString("stage", HifzStage.LEARNING.name)) }
                    .getOrDefault(HifzStage.LEARNING)
                val surah = item.optInt("surah", 1).coerceIn(1, QuranMetadata.TOTAL_SURAHS)
                val maxAyah = QuranMetadata.surah(surah).ayahCount
                val fromAyah = item.optInt("fromAyah", 1).coerceIn(1, maxAyah)
                val toAyah = item.optInt("toAyah", fromAyah).coerceIn(fromAyah, maxAyah)
                add(
                    HifzItem(
                        surah = surah,
                        fromAyah = fromAyah,
                        toAyah = toAyah,
                        ayahRepeat = item.optInt("ayahRepeat", 5).coerceIn(1, 20),
                        rangeRepeat = item.optInt("rangeRepeat", 3).coerceIn(1, 20),
                        stage = stage,
                        reviewLevel = item.optInt("reviewLevel", 0).coerceAtLeast(0),
                        nextReviewEpochDay = item.optLong("nextReviewEpochDay", LocalDate.now().toEpochDay()),
                    )
                )
            }
        }.filter { it.toAyah >= it.fromAyah }

        val khatmaJson = json.optJSONObject("khatma") ?: JSONObject()
        val khatma = KhatmaProgress(
            active = khatmaJson.optBoolean("active", false),
            goalDays = khatmaJson.optInt("goalDays", 30).coerceIn(1, 365),
            startedEpochDay = khatmaJson.optLong("startedEpochDay", 0L).coerceAtLeast(0L),
            completedPages = khatmaJson.optJSONArray("completedPages").toIntSet(),
            todayPages = khatmaJson.optJSONArray("todayPages").toIntSet(),
        )

        val adhkar = mutableMapOf<String, Int>()
        val counts = json.optJSONObject("adhkarCounts") ?: JSONObject()
        val keys = counts.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            adhkar[key] = counts.optInt(key, 0).coerceAtLeast(0)
        }
        return PracticeState(items, khatma, adhkar)
    }

    private fun parseStats(json: JSONObject): StatsBackupData {
        val daily = mutableMapOf<Long, Int>()
        val counts = json.optJSONObject("dailyPageCounts") ?: JSONObject()
        val keys = counts.keys()
        while (keys.hasNext()) {
            val raw = keys.next()
            val day = raw.toLongOrNull() ?: continue
            daily[day] = counts.optInt(raw, 0).coerceAtLeast(0)
        }
        return StatsBackupData(
            totalPageViews = json.optInt("totalPageViews", 0).coerceAtLeast(0),
            uniquePages = json.optJSONArray("uniquePages").toIntSet(),
            audioSeconds = json.optLong("audioSeconds", 0L).coerceAtLeast(0L),
            readingDays = json.optJSONArray("readingDays").toLongSet(),
            dailyPageCounts = daily,
        )
    }

    private fun parseLocation(json: JSONObject) = AppLocation(
        latitude = json.optDouble("latitude", 30.0444).coerceIn(-90.0, 90.0),
        longitude = json.optDouble("longitude", 31.2357).coerceIn(-180.0, 180.0),
        label = json.optString("label", "موقع محفوظ").ifBlank { "موقع محفوظ" },
    )

    private fun Iterable<*>.toJsonArray() = JSONArray().also { array -> forEach { array.put(it) } }

    private fun JSONArray?.toIntSet(): Set<Int> {
        if (this == null) return emptySet()
        return buildSet { for (i in 0 until length()) add(optInt(i)) }
    }

    private fun JSONArray?.toLongSet(): Set<Long> {
        if (this == null) return emptySet()
        return buildSet { for (i in 0 until length()) add(optLong(i)) }
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet { for (i in 0 until length()) optString(i).takeIf(String::isNotBlank)?.let(::add) }
    }
}
