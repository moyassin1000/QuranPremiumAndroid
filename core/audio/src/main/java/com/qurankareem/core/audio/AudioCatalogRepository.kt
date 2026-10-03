package com.qurankareem.core.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AudioCatalogRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "mp3quran_reciters_ar.json")

    suspend fun reciters(forceRefresh: Boolean = false): List<QuranReciter> = withContext(Dispatchers.IO) {
        val json = if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0L) {
            cacheFile.readText(Charsets.UTF_8)
        } else {
            downloadCatalog().also { cacheFile.writeText(it, Charsets.UTF_8) }
        }
        parse(json)
    }

    private fun downloadCatalog(): String {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "QuranPremium/1.6")
        }
        return try {
            if (connection.responseCode !in 200..299) error("تعذر تحميل قائمة القراء")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: String): List<QuranReciter> {
        val root = JSONObject(json)
        val source = root.optJSONArray("reciters") ?: return emptyList()
        val result = ArrayList<QuranReciter>(source.length())
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            val moshafs = item.optJSONArray("moshaf") ?: continue
            var selected: QuranMoshaf? = null
            for (j in 0 until moshafs.length()) {
                val m = moshafs.optJSONObject(j) ?: continue
                val surahs = m.optString("surah_list")
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
                if (surahs.isEmpty()) continue
                val candidate = QuranMoshaf(
                    id = m.optInt("id"),
                    name = m.optString("name"),
                    server = m.optString("server"),
                    surahNumbers = surahs,
                )
                // Prefer a complete Hafs/murattal collection when available.
                if (selected == null || candidate.surahNumbers.size > selected.surahNumbers.size) selected = candidate
            }
            val moshaf = selected ?: continue
            result += QuranReciter(
                id = item.optInt("id"),
                name = item.optString("name"),
                letter = item.optString("letter"),
                moshaf = moshaf,
            )
        }
        return result.sortedBy { it.name }
    }

    companion object {
        const val API_URL = "https://www.mp3quran.net/api/v3/reciters?language=ar"
        const val SOURCE_NAME = "MP3Quran.net API v3"
    }
}
