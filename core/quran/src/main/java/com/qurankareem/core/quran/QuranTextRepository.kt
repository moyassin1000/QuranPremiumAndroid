package com.qurankareem.core.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

data class QuranAyah(
    val surah: Int,
    val ayah: Int,
    val text: String,
) {
    val key: String get() = QuranMetadata.ayahKey(surah, ayah)
}

data class QuranTextResult(
    val ayahs: List<QuranAyah>,
    val fromCache: Boolean,
)

data class QuranSearchHit(
    val ayah: QuranAyah,
    val surahName: String,
    val page: Int,
)

class QuranTextRepository(context: Context) {
    private val cacheDir = File(context.filesDir, "quran_text_hafs").apply { mkdirs() }
    private val wholeQuranFile = File(cacheDir, "quran_hafs_full.txt")

    @Volatile
    private var wholeQuranMemory: List<QuranAyah>? = null

    suspend fun page(page: Int): QuranTextResult {
        val safe = page.coerceIn(1, QuranMetadata.TOTAL_PAGES)
        val start = QuranMetadata.pageStart(safe)
        val end = if (safe < QuranMetadata.TOTAL_PAGES) QuranMetadata.pageStart(safe + 1) else null
        return loadScoped(
            cacheKey = "page_${safe}.txt",
            query = "page=$safe",
            start = start,
            endExclusive = end,
        )
    }

    suspend fun surah(surah: Int): QuranTextResult {
        val safe = surah.coerceIn(1, QuranMetadata.TOTAL_SURAHS)
        val start = AyahRef(safe, 1)
        val end = if (safe < QuranMetadata.TOTAL_SURAHS) AyahRef(safe + 1, 1) else null
        return loadScoped(
            cacheKey = "surah_${safe}.txt",
            query = "surah=$safe",
            start = start,
            endExclusive = end,
        )
    }

    suspend fun juz(juz: Int): QuranTextResult {
        val safe = juz.coerceIn(1, QuranMetadata.TOTAL_JUZ)
        val start = QuranMetadata.juzStart(safe)
        val end = if (safe < QuranMetadata.TOTAL_JUZ) QuranMetadata.juzStart(safe + 1) else null
        return loadScoped(
            cacheKey = "juz_${safe}.txt",
            query = "juz=$safe",
            start = start,
            endExclusive = end,
        )
    }

    suspend fun downloadOfflinePack(force: Boolean = false): QuranTextResult = withContext(Dispatchers.IO) {
        if (!force && wholeQuranFile.exists() && wholeQuranFile.length() > 0L) {
            val cached = wholeQuranMemory ?: parse(wholeQuranFile.readText(Charsets.UTF_8)).also { wholeQuranMemory = it }
            return@withContext QuranTextResult(cached, fromCache = true)
        }
        val body = download("")
        val ayahs = parse(body)
        check(ayahs.size == QuranMetadata.TOTAL_AYAHS) {
            "عدد الآيات المحمّلة غير متوقع (${ayahs.size})"
        }
        wholeQuranFile.writeText(body, Charsets.UTF_8)
        wholeQuranMemory = ayahs
        QuranTextResult(ayahs, fromCache = false)
    }

    fun offlinePackReady(): Boolean = wholeQuranFile.exists() && wholeQuranFile.length() > 0L

    fun offlinePackSizeBytes(): Long = wholeQuranFile.takeIf { it.exists() }?.length() ?: 0L

    suspend fun search(query: String, maxResults: Int = 100): List<QuranSearchHit> = withContext(Dispatchers.IO) {
        val needle = normalizeForSearch(query)
        if (needle.length < 2) return@withContext emptyList()
        val all = downloadOfflinePack().ayahs
        all.asSequence()
            .filter { normalizeForSearch(it.text).contains(needle) }
            .take(maxResults.coerceIn(1, 250))
            .map { ayah ->
                QuranSearchHit(
                    ayah = ayah,
                    surahName = QuranMetadata.surah(ayah.surah).nameArabic,
                    page = QuranMetadata.pageForAyah(ayah.surah, ayah.ayah),
                )
            }
            .toList()
    }

    private suspend fun loadScoped(
        cacheKey: String,
        query: String,
        start: AyahRef,
        endExclusive: AyahRef?,
    ): QuranTextResult = withContext(Dispatchers.IO) {
        val file = File(cacheDir, cacheKey)
        if (file.exists() && file.length() > 0L) {
            return@withContext QuranTextResult(parse(file.readText(Charsets.UTF_8)), fromCache = true)
        }

        if (offlinePackReady()) {
            val all = wholeQuranMemory ?: parse(wholeQuranFile.readText(Charsets.UTF_8)).also { wholeQuranMemory = it }
            val selected = slice(all, start, endExclusive)
            if (selected.isNotEmpty()) {
                file.writeText(serialize(selected), Charsets.UTF_8)
                return@withContext QuranTextResult(selected, fromCache = true)
            }
        }

        val body = download(query)
        val ayahs = parse(body)
        if (ayahs.isEmpty()) error("وصل رد فارغ من مصدر النص")
        file.writeText(body, Charsets.UTF_8)
        QuranTextResult(ayahs, fromCache = false)
    }

    private fun slice(all: List<QuranAyah>, start: AyahRef, endExclusive: AyahRef?): List<QuranAyah> {
        val startOrdinal = QuranMetadata.ordinal(start.surah, start.ayah)
        val endOrdinal = endExclusive?.let { QuranMetadata.ordinal(it.surah, it.ayah) } ?: Int.MAX_VALUE
        return all.filter { ayah ->
            val ordinal = QuranMetadata.ordinal(ayah.surah, ayah.ayah)
            ordinal in startOrdinal until endOrdinal
        }
    }

    private fun download(query: String): String {
        val urlText = buildString {
            append("https://text.quran.ws/download?edition=hafs&format=txt")
            if (query.isNotBlank()) append('&').append(query)
        }
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "text/plain; charset=utf-8")
            setRequestProperty("User-Agent", "QuranPremium/1.4.0")
        }
        try {
            if (connection.responseCode !in 200..299) error("تعذر تحميل النص القرآني الآن")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): List<QuranAyah> = body.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split('|', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val surah = parts[0].toIntOrNull() ?: return@mapNotNull null
            val ayah = parts[1].toIntOrNull() ?: return@mapNotNull null
            QuranAyah(surah, ayah, parts[2])
        }
        .toList()

    private fun serialize(ayahs: List<QuranAyah>): String = ayahs.joinToString("\n") {
        "${it.surah}|${it.ayah}|${it.text}"
    }

    private fun normalizeForSearch(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{M}+"), "")
            .replace("ـ", "")
            .replace('ٱ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun clearCache(keepOfflinePack: Boolean = true) {
        cacheDir.listFiles()?.forEach { file ->
            if (!keepOfflinePack || file != wholeQuranFile) file.delete()
        }
        if (!keepOfflinePack) wholeQuranMemory = null
    }

    companion object {
        const val SOURCE_NAME = "Quran.ws / KFGQPC Hafs Uthmanic text"
        const val SOURCE_URL = "https://text.quran.ws/"
    }
}
