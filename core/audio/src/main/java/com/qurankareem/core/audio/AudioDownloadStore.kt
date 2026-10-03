package com.qurankareem.core.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File

data class DownloadedAudio(
    val reciterId: Int,
    val moshafId: Int,
    val surah: Int,
    val file: File,
    val sizeBytes: Long,
)

enum class AudioDownloadState { NONE, QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class AudioDownloadProgress(
    val reciterId: Int,
    val moshafId: Int,
    val surah: Int,
    val state: AudioDownloadState,
    val percent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
)

class AudioDownloadStore(private val context: Context) {
    private val baseDir = File(context.filesDir, "quran_audio").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("quran_audio_downloads", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(context)

    fun fileFor(reciterId: Int, moshafId: Int, surah: Int): File =
        File(baseDir, "${reciterId}_${moshafId}_${surah.toString().padStart(3, '0')}.mp3")

    fun playableUri(reciter: QuranReciter, surah: Int): String {
        val local = fileFor(reciter.id, reciter.moshaf.id, surah)
        return if (local.exists() && local.length() > 0L) local.toURI().toString() else reciter.moshaf.surahUrl(surah)
    }

    fun isDownloaded(reciter: QuranReciter, surah: Int): Boolean =
        fileFor(reciter.id, reciter.moshaf.id, surah).let { it.exists() && it.length() > 0L }

    fun enqueue(reciter: QuranReciter, surah: Int, wifiOnly: Boolean = false) {
        val target = fileFor(reciter.id, reciter.moshaf.id, surah)
        val key = key(reciter.id, reciter.moshaf.id, surah)
        writeProgress(key, AudioDownloadState.QUEUED, 0, target.takeIf { it.exists() }?.length() ?: 0L, 0L)
        val data = Data.Builder()
            .putString(QuranAudioDownloadWorker.KEY_URL, reciter.moshaf.surahUrl(surah))
            .putString(QuranAudioDownloadWorker.KEY_PATH, target.absolutePath)
            .putString(QuranAudioDownloadWorker.KEY_PROGRESS_KEY, key)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequestBuilder<QuranAudioDownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(key)
            .build()
        workManager.enqueueUniqueWork(key, ExistingWorkPolicy.REPLACE, work)
    }

    fun cancel(reciterId: Int, moshafId: Int, surah: Int) {
        val key = key(reciterId, moshafId, surah)
        workManager.cancelUniqueWork(key)
        val target = fileFor(reciterId, moshafId, surah)
        File(target.absolutePath + ".part").delete()
        clearProgress(key)
    }

    fun progress(reciterId: Int, moshafId: Int, surah: Int): AudioDownloadProgress {
        val key = key(reciterId, moshafId, surah)
        val target = fileFor(reciterId, moshafId, surah)
        if (target.exists() && target.length() > 0L) {
            return AudioDownloadProgress(reciterId, moshafId, surah, AudioDownloadState.COMPLETED, 100, target.length(), target.length())
        }
        val state = runCatching { AudioDownloadState.valueOf(prefs.getString("$key.state", "NONE") ?: "NONE") }.getOrDefault(AudioDownloadState.NONE)
        return AudioDownloadProgress(
            reciterId = reciterId,
            moshafId = moshafId,
            surah = surah,
            state = state,
            percent = prefs.getInt("$key.percent", 0).coerceIn(0, 100),
            downloadedBytes = prefs.getLong("$key.done", 0L),
            totalBytes = prefs.getLong("$key.total", 0L),
        )
    }

    fun activeProgress(): List<AudioDownloadProgress> = prefs.all.keys
        .filter { it.endsWith(".state") }
        .map { it.removeSuffix(".state") }
        .distinct()
        .mapNotNull { token ->
            val parts = token.removePrefix("quran_audio_").split('_')
            if (parts.size != 3) return@mapNotNull null
            val r = parts[0].toIntOrNull() ?: return@mapNotNull null
            val m = parts[1].toIntOrNull() ?: return@mapNotNull null
            val s = parts[2].toIntOrNull() ?: return@mapNotNull null
            progress(r, m, s).takeIf { it.state != AudioDownloadState.NONE && it.state != AudioDownloadState.COMPLETED }
        }
        .sortedWith(compareBy({ it.reciterId }, { it.surah }))

    fun downloaded(): List<DownloadedAudio> = baseDir.listFiles()
        .orEmpty()
        .filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) && it.length() > 0L }
        .mapNotNull { file ->
            val parts = file.nameWithoutExtension.split('_')
            if (parts.size != 3) return@mapNotNull null
            val reciterId = parts[0].toIntOrNull() ?: return@mapNotNull null
            val moshafId = parts[1].toIntOrNull() ?: return@mapNotNull null
            val surah = parts[2].toIntOrNull()?.takeIf { it in 1..114 } ?: return@mapNotNull null
            DownloadedAudio(reciterId, moshafId, surah, file, file.length())
        }
        .sortedWith(compareBy({ it.reciterId }, { it.surah }))

    fun totalDownloadedBytes(): Long = downloaded().sumOf { it.sizeBytes }

    fun delete(item: DownloadedAudio): Boolean {
        clearProgress(key(item.reciterId, item.moshafId, item.surah))
        return item.file.delete()
    }

    fun deleteAll(): Int {
        var count = 0
        downloaded().forEach { if (delete(it)) count++ }
        return count
    }

    private fun key(reciterId: Int, moshafId: Int, surah: Int) = "quran_audio_${reciterId}_${moshafId}_${surah}"

    private fun writeProgress(key: String, state: AudioDownloadState, percent: Int, done: Long, total: Long) {
        prefs.edit()
            .putString("$key.state", state.name)
            .putInt("$key.percent", percent.coerceIn(0, 100))
            .putLong("$key.done", done)
            .putLong("$key.total", total)
            .apply()
    }

    private fun clearProgress(key: String) {
        prefs.edit().remove("$key.state").remove("$key.percent").remove("$key.done").remove("$key.total").apply()
    }
}
