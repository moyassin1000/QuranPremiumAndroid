package com.qurankareem.core.audio

data class QuranMoshaf(
    val id: Int,
    val name: String,
    val server: String,
    val surahNumbers: Set<Int>,
)

data class QuranReciter(
    val id: Int,
    val name: String,
    val letter: String,
    val moshaf: QuranMoshaf,
)

fun QuranMoshaf.surahUrl(surah: Int): String {
    val base = if (server.endsWith('/')) server else "$server/"
    return base + surah.coerceIn(1, 114).toString().padStart(3, '0') + ".mp3"
}
