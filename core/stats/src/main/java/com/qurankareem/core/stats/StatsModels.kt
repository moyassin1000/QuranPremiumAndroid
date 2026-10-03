package com.qurankareem.core.stats

import java.time.LocalDate

data class DailyReadingStat(
    val epochDay: Long,
    val pages: Int,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}

data class AppStats(
    val totalPageViews: Int = 0,
    val uniquePagesRead: Int = 0,
    val audioSeconds: Long = 0L,
    val readingDays: Set<Long> = emptySet(),
    val recentReading: List<DailyReadingStat> = emptyList(),
) {
    val audioMinutes: Long get() = audioSeconds / 60L
    val readingStreak: Int get() = computeStreak(readingDays, LocalDate.now().toEpochDay())
    val last7DaysPages: Int get() = recentReading.sumOf { it.pages }
}

fun computeStreak(days: Set<Long>, today: Long): Int {
    if (days.isEmpty()) return 0
    var cursor = today
    if (cursor !in days && cursor - 1 in days) cursor--
    var streak = 0
    while (cursor in days) {
        streak++
        cursor--
    }
    return streak
}
