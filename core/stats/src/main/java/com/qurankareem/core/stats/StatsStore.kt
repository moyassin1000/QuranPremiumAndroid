package com.qurankareem.core.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.statsDataStore by preferencesDataStore(name = "usage_stats_v1")

data class StatsBackupData(
    val totalPageViews: Int = 0,
    val uniquePages: Set<Int> = emptySet(),
    val audioSeconds: Long = 0L,
    val readingDays: Set<Long> = emptySet(),
    val dailyPageCounts: Map<Long, Int> = emptyMap(),
)

class StatsStore(private val context: Context) {
    private val totalPageViewsKey = intPreferencesKey("total_page_views")
    private val uniquePagesKey = stringSetPreferencesKey("unique_pages")
    private val audioSecondsKey = longPreferencesKey("audio_seconds")
    private val readingDaysKey = stringSetPreferencesKey("reading_days")
    private val dailyCountsKey = stringSetPreferencesKey("daily_page_counts")

    val state: Flow<AppStats> = context.statsDataStore.data.map { prefs ->
        val today = LocalDate.now().toEpochDay()
        val recent = parseDailyCounts(prefs[dailyCountsKey])
            .filterKeys { it in (today - 6)..today }
            .map { DailyReadingStat(it.key, it.value) }
            .sortedBy { it.epochDay }
        AppStats(
            totalPageViews = prefs[totalPageViewsKey] ?: 0,
            uniquePagesRead = prefs[uniquePagesKey].orEmpty().size,
            audioSeconds = prefs[audioSecondsKey] ?: 0L,
            readingDays = prefs[readingDaysKey].orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
            recentReading = recent,
        )
    }

    suspend fun recordPageView(page: Int) {
        val safePage = page.coerceIn(1, 604)
        val today = LocalDate.now().toEpochDay()
        context.statsDataStore.edit { prefs ->
            prefs[totalPageViewsKey] = (prefs[totalPageViewsKey] ?: 0) + 1
            prefs[uniquePagesKey] = prefs[uniquePagesKey].orEmpty().toMutableSet().apply { add(safePage.toString()) }
            prefs[readingDaysKey] = prefs[readingDaysKey].orEmpty().toMutableSet().apply { add(today.toString()) }
            val counts = parseDailyCounts(prefs[dailyCountsKey]).toMutableMap()
            counts[today] = (counts[today] ?: 0) + 1
            val cutoff = today - 60
            prefs[dailyCountsKey] = counts.filterKeys { it >= cutoff }.map { "${it.key}=${it.value}" }.toSet()
        }
    }

    suspend fun addAudioSeconds(seconds: Long) {
        if (seconds <= 0L) return
        context.statsDataStore.edit { prefs ->
            prefs[audioSecondsKey] = (prefs[audioSecondsKey] ?: 0L) + seconds
        }
    }


    suspend fun exportBackup(): StatsBackupData {
        val prefs = context.statsDataStore.data.first()
        return StatsBackupData(
            totalPageViews = (prefs[totalPageViewsKey] ?: 0).coerceAtLeast(0),
            uniquePages = prefs[uniquePagesKey].orEmpty().mapNotNull { it.toIntOrNull() }.filter { it in 1..604 }.toSet(),
            audioSeconds = (prefs[audioSecondsKey] ?: 0L).coerceAtLeast(0L),
            readingDays = prefs[readingDaysKey].orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
            dailyPageCounts = parseDailyCounts(prefs[dailyCountsKey]).filterValues { it >= 0 },
        )
    }

    suspend fun replaceBackup(data: StatsBackupData) {
        context.statsDataStore.edit { prefs ->
            prefs[totalPageViewsKey] = data.totalPageViews.coerceAtLeast(0)
            prefs[uniquePagesKey] = data.uniquePages.filter { it in 1..604 }.map(Int::toString).toSet()
            prefs[audioSecondsKey] = data.audioSeconds.coerceAtLeast(0L)
            prefs[readingDaysKey] = data.readingDays.map(Long::toString).toSet()
            prefs[dailyCountsKey] = data.dailyPageCounts
                .filterValues { it >= 0 }
                .map { "${it.key}=${it.value}" }
                .toSet()
        }
    }

    private fun parseDailyCounts(raw: Set<String>?): Map<Long, Int> = raw.orEmpty().mapNotNull { token ->
        val idx = token.lastIndexOf('=')
        if (idx <= 0) null else {
            val day = token.substring(0, idx).toLongOrNull()
            val count = token.substring(idx + 1).toIntOrNull()
            if (day == null || count == null) null else day to count
        }
    }.toMap()
}
