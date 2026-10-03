package com.qurankareem.core.audio

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class HifzReciter(
    val id: String,
    val name: String,
    val folder: String,
)

object HifzReciters {
    val all = listOf(
        HifzReciter("alafasy", "مشاري راشد العفاسي", "Alafasy_128kbps"),
        HifzReciter("husary", "محمود خليل الحصري", "Husary_128kbps"),
        HifzReciter("abdulbasit", "عبد الباسط عبد الصمد", "Abdul_Basit_Murattal_192kbps"),
        HifzReciter("sudais", "عبد الرحمن السديس", "Abdurrahmaan_As-Sudais_192kbps"),
    )

    fun byId(id: String): HifzReciter = all.firstOrNull { it.id == id } ?: all.first()
}

fun HifzReciter.ayahUrl(surah: Int, ayah: Int): String =
    "https://everyayah.com/data/$folder/${surah.coerceIn(1, 114).toString().padStart(3, '0')}${ayah.coerceAtLeast(1).toString().padStart(3, '0')}.mp3"

fun buildHifzMediaItems(
    reciter: HifzReciter,
    surah: Int,
    fromAyah: Int,
    toAyah: Int,
    ayahRepeat: Int,
    rangeRepeat: Int,
    surahName: String,
): List<MediaItem> {
    val start = fromAyah.coerceAtLeast(1)
    val end = toAyah.coerceAtLeast(start)
    val perAyah = ayahRepeat.coerceIn(1, 20)
    val cycles = rangeRepeat.coerceIn(1, 20)
    val result = ArrayList<MediaItem>()
    repeat(cycles) { cycle ->
        for (ayah in start..end) {
            repeat(perAyah) { repeatIndex ->
                result += MediaItem.Builder()
                    .setMediaId("hifz:${reciter.id}:$surah:$ayah:${cycle + 1}:${repeatIndex + 1}")
                    .setUri(reciter.ayahUrl(surah, ayah))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("سورة $surahName • الآية $ayah")
                            .setArtist("${reciter.name} • الحفظ")
                            .build()
                    )
                    .build()
            }
        }
    }
    return result
}

data class HifzPlaybackProgress(
    val reciterId: String,
    val surah: Int,
    val ayah: Int,
    val cycle: Int,
    val repeatIndex: Int,
)

fun parseHifzMediaId(mediaId: String?): HifzPlaybackProgress? {
    if (mediaId.isNullOrBlank() || !mediaId.startsWith("hifz:")) return null
    val parts = mediaId.split(':')
    if (parts.size != 6) return null
    return runCatching {
        HifzPlaybackProgress(
            reciterId = parts[1],
            surah = parts[2].toInt(),
            ayah = parts[3].toInt(),
            cycle = parts[4].toInt(),
            repeatIndex = parts[5].toInt(),
        )
    }.getOrNull()
}
