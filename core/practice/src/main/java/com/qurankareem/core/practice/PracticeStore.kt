package com.qurankareem.core.practice

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.practiceDataStore by preferencesDataStore(name = "practice_state")

class PracticeStore(private val context: Context) {
    private val hifzItemsKey = stringSetPreferencesKey("hifz_items_v1")
    private val khatmaActiveKey = booleanPreferencesKey("khatma_active")
    private val khatmaGoalDaysKey = intPreferencesKey("khatma_goal_days")
    private val khatmaStartedKey = longPreferencesKey("khatma_started_epoch_day")
    private val khatmaPagesKey = stringSetPreferencesKey("khatma_completed_pages")
    private val todayDateKey = stringPreferencesKey("wird_today_date")
    private val todayPagesKey = stringSetPreferencesKey("wird_today_pages")
    private val adhkarDateKey = stringPreferencesKey("adhkar_date")
    private val adhkarCountsKey = stringSetPreferencesKey("adhkar_counts")

    val state: Flow<PracticeState> = context.practiceDataStore.data.map { prefs ->
        val today = LocalDate.now().toString()
        val savedTodayPages = if (prefs[todayDateKey] == today) parseIntSet(prefs[todayPagesKey]) else emptySet()
        val savedAdhkarCounts = if (prefs[adhkarDateKey] == today) parseCounts(prefs[adhkarCountsKey]) else emptyMap()
        PracticeState(
            hifzItems = prefs[hifzItemsKey].orEmpty().mapNotNull(::decodeHifz).sortedWith(compareBy({ it.surah }, { it.fromAyah })),
            khatma = KhatmaProgress(
                active = prefs[khatmaActiveKey] ?: false,
                goalDays = (prefs[khatmaGoalDaysKey] ?: 30).coerceIn(1, 365),
                startedEpochDay = prefs[khatmaStartedKey] ?: 0L,
                completedPages = parseIntSet(prefs[khatmaPagesKey]).filter { it in 1..604 }.toSet(),
                todayPages = savedTodayPages.filter { it in 1..604 }.toSet(),
            ),
            adhkarCounts = savedAdhkarCounts,
        )
    }

    suspend fun addHifzPlan(
        surah: Int,
        fromAyah: Int,
        toAyah: Int,
        ayahRepeat: Int,
        rangeRepeat: Int,
    ) {
        val safeSurah = surah.coerceIn(1, 114)
        val safeFrom = fromAyah.coerceAtLeast(1)
        val safeTo = toAyah.coerceAtLeast(safeFrom)
        val item = HifzItem(
            surah = safeSurah,
            fromAyah = safeFrom,
            toAyah = safeTo,
            ayahRepeat = ayahRepeat.coerceIn(1, 20),
            rangeRepeat = rangeRepeat.coerceIn(1, 20),
        )
        context.practiceDataStore.edit { prefs ->
            val items = prefs[hifzItemsKey].orEmpty().mapNotNull(::decodeHifz).associateBy { it.key }.toMutableMap()
            items[item.key] = item
            prefs[hifzItemsKey] = items.values.map(::encodeHifz).toSet()
        }
    }

    suspend fun markHifzReviewed(key: String, success: Boolean) {
        context.practiceDataStore.edit { prefs ->
            val items = prefs[hifzItemsKey].orEmpty().mapNotNull(::decodeHifz).toMutableList()
            val index = items.indexOfFirst { it.key == key }
            if (index < 0) return@edit
            val current = items[index]
            val next = if (success) {
                val newLevel = (current.reviewLevel + 1).coerceAtMost(REVIEW_INTERVALS.lastIndex)
                val mastered = newLevel >= REVIEW_INTERVALS.lastIndex
                current.copy(
                    stage = if (mastered) HifzStage.MASTERED else HifzStage.REVIEW,
                    reviewLevel = newLevel,
                    nextReviewEpochDay = LocalDate.now().plusDays(REVIEW_INTERVALS[newLevel].toLong()).toEpochDay(),
                )
            } else {
                current.copy(
                    stage = HifzStage.REVIEW,
                    reviewLevel = 0,
                    nextReviewEpochDay = LocalDate.now().plusDays(1).toEpochDay(),
                )
            }
            items[index] = next
            prefs[hifzItemsKey] = items.map(::encodeHifz).toSet()
        }
    }

    suspend fun removeHifzPlan(key: String) {
        context.practiceDataStore.edit { prefs ->
            prefs[hifzItemsKey] = prefs[hifzItemsKey].orEmpty().mapNotNull(::decodeHifz).filterNot { it.key == key }.map(::encodeHifz).toSet()
        }
    }

    suspend fun startKhatma(goalDays: Int) {
        context.practiceDataStore.edit { prefs ->
            prefs[khatmaActiveKey] = true
            prefs[khatmaGoalDaysKey] = goalDays.coerceIn(1, 365)
            prefs[khatmaStartedKey] = LocalDate.now().toEpochDay()
            prefs[khatmaPagesKey] = emptySet()
            prefs[todayDateKey] = LocalDate.now().toString()
            prefs[todayPagesKey] = emptySet()
        }
    }

    suspend fun stopKhatma() {
        context.practiceDataStore.edit { it[khatmaActiveKey] = false }
    }

    suspend fun recordPageRead(page: Int) {
        val safePage = page.coerceIn(1, 604)
        context.practiceDataStore.edit { prefs ->
            if (prefs[khatmaActiveKey] != true) return@edit
            val today = LocalDate.now().toString()
            if (prefs[todayDateKey] != today) {
                prefs[todayDateKey] = today
                prefs[todayPagesKey] = emptySet()
            }
            val all = prefs[khatmaPagesKey].orEmpty().toMutableSet().apply { add(safePage.toString()) }
            val daily = prefs[todayPagesKey].orEmpty().toMutableSet().apply { add(safePage.toString()) }
            prefs[khatmaPagesKey] = all
            prefs[todayPagesKey] = daily
        }
    }

    suspend fun incrementDhikr(id: String, target: Int) {
        context.practiceDataStore.edit { prefs ->
            val today = LocalDate.now().toString()
            if (prefs[adhkarDateKey] != today) {
                prefs[adhkarDateKey] = today
                prefs[adhkarCountsKey] = emptySet()
            }
            val counts = parseCounts(prefs[adhkarCountsKey]).toMutableMap()
            counts[id] = ((counts[id] ?: 0) + 1).coerceAtMost(target.coerceAtLeast(1))
            prefs[adhkarCountsKey] = counts.map { "${it.key}=${it.value}" }.toSet()
        }
    }

    suspend fun resetDhikr(id: String) {
        context.practiceDataStore.edit { prefs ->
            val counts = parseCounts(prefs[adhkarCountsKey]).toMutableMap()
            counts.remove(id)
            prefs[adhkarCountsKey] = counts.map { "${it.key}=${it.value}" }.toSet()
        }
    }


    suspend fun replaceState(state: PracticeState, restoreDailyState: Boolean) {
        context.practiceDataStore.edit { prefs ->
            prefs[hifzItemsKey] = state.hifzItems
                .filter { it.surah in 1..114 && it.fromAyah >= 1 && it.toAyah >= it.fromAyah }
                .map(::encodeHifz)
                .toSet()
            prefs[khatmaActiveKey] = state.khatma.active
            prefs[khatmaGoalDaysKey] = state.khatma.goalDays.coerceIn(1, 365)
            prefs[khatmaStartedKey] = state.khatma.startedEpochDay.coerceAtLeast(0L)
            prefs[khatmaPagesKey] = state.khatma.completedPages.filter { it in 1..604 }.map(Int::toString).toSet()
            prefs[todayDateKey] = LocalDate.now().toString()
            prefs[todayPagesKey] = if (restoreDailyState) {
                state.khatma.todayPages.filter { it in 1..604 }.map(Int::toString).toSet()
            } else {
                emptySet()
            }
            prefs[adhkarDateKey] = LocalDate.now().toString()
            prefs[adhkarCountsKey] = if (restoreDailyState) {
                state.adhkarCounts.filterValues { it >= 0 }.map { "${it.key}=${it.value}" }.toSet()
            } else {
                emptySet()
            }
        }
    }

    private fun parseIntSet(raw: Set<String>?): Set<Int> = raw.orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    private fun parseCounts(raw: Set<String>?): Map<String, Int> = raw.orEmpty().mapNotNull { token ->
        val split = token.lastIndexOf('=')
        if (split <= 0) null else token.substring(0, split) to (token.substring(split + 1).toIntOrNull() ?: 0)
    }.toMap()

    private fun encodeHifz(item: HifzItem): String = listOf(
        item.surah,
        item.fromAyah,
        item.toAyah,
        item.ayahRepeat,
        item.rangeRepeat,
        item.stage.name,
        item.reviewLevel,
        item.nextReviewEpochDay,
    ).joinToString("|")

    private fun decodeHifz(raw: String): HifzItem? {
        val p = raw.split('|')
        if (p.size != 8) return null
        return runCatching {
            HifzItem(
                surah = p[0].toInt(),
                fromAyah = p[1].toInt(),
                toAyah = p[2].toInt(),
                ayahRepeat = p[3].toInt(),
                rangeRepeat = p[4].toInt(),
                stage = HifzStage.valueOf(p[5]),
                reviewLevel = p[6].toInt(),
                nextReviewEpochDay = p[7].toLong(),
            )
        }.getOrNull()
    }

    private companion object {
        val REVIEW_INTERVALS = intArrayOf(1, 1, 3, 7, 14, 30)
    }
}
