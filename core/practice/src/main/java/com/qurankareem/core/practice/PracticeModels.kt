package com.qurankareem.core.practice

import java.time.LocalDate
import kotlin.math.ceil

enum class HifzStage { LEARNING, REVIEW, MASTERED }

data class HifzItem(
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val ayahRepeat: Int = 5,
    val rangeRepeat: Int = 3,
    val stage: HifzStage = HifzStage.LEARNING,
    val reviewLevel: Int = 0,
    val nextReviewEpochDay: Long = LocalDate.now().toEpochDay(),
) {
    val key: String get() = "$surah:$fromAyah-$toAyah"
    val isDue: Boolean get() = nextReviewEpochDay <= LocalDate.now().toEpochDay()
}

data class KhatmaProgress(
    val active: Boolean = false,
    val goalDays: Int = 30,
    val startedEpochDay: Long = 0L,
    val completedPages: Set<Int> = emptySet(),
    val todayPages: Set<Int> = emptySet(),
) {
    val dailyGoal: Int get() = ceil(604.0 / goalDays.coerceAtLeast(1)).toInt()
    val perPrayerGoal: Int get() = ceil(dailyGoal / 5.0).toInt()
    val completedCount: Int get() = completedPages.size.coerceAtMost(604)
    val remainingPages: Int get() = (604 - completedCount).coerceAtLeast(0)
    val percent: Float get() = completedCount / 604f
    val todayCount: Int get() = todayPages.size
    val todayRemaining: Int get() = (dailyGoal - todayCount).coerceAtLeast(0)
    val nextPage: Int get() = (1..604).firstOrNull { it !in completedPages } ?: 604
}

data class PracticeState(
    val hifzItems: List<HifzItem> = emptyList(),
    val khatma: KhatmaProgress = KhatmaProgress(),
    val adhkarCounts: Map<String, Int> = emptyMap(),
) {
    val dueReviews: List<HifzItem> get() = hifzItems.filter { it.isDue && it.stage != HifzStage.MASTERED }
}

