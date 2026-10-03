package com.qurankareem.core.audio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class QuranAudioDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val prefs = appContext.getSharedPreferences("quran_audio_downloads", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val path = inputData.getString(KEY_PATH) ?: return@withContext Result.failure()
        val progressKey = inputData.getString(KEY_PROGRESS_KEY) ?: return@withContext Result.failure()
        val target = File(path)
        val temp = File(path + ".part")
        target.parentFile?.mkdirs()

        try {
            val existing = temp.takeIf { it.exists() }?.length() ?: 0L
            var connection = open(url, existing)
            var response = connection.responseCode
            var append = existing > 0L && response == HttpURLConnection.HTTP_PARTIAL
            if (existing > 0L && !append) {
                connection.disconnect()
                temp.delete()
                connection = open(url, 0L)
                response = connection.responseCode
            }
            try {
                if (response !in 200..299) {
                    write(progressKey, AudioDownloadState.FAILED, 0, temp.length(), 0L)
                    return@withContext Result.retry()
                }
                val base = if (append) existing else 0L
                val contentLength = connection.contentLengthLong.coerceAtLeast(0L)
                val total = if (contentLength > 0L) base + contentLength else 0L
                write(progressKey, AudioDownloadState.DOWNLOADING, percent(base, total), base, total)

                connection.inputStream.use { input ->
                    RandomAccessFile(temp, "rw").use { output ->
                        if (append) output.seek(existing) else output.setLength(0L)
                        val buffer = ByteArray(64 * 1024)
                        var done = base
                        var lastPercent = -1
                        while (coroutineContext.isActive) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            done += read
                            val pct = percent(done, total)
                            if (pct != lastPercent) {
                                write(progressKey, AudioDownloadState.DOWNLOADING, pct, done, total)
                                lastPercent = pct
                            }
                        }
                        if (!coroutineContext.isActive) return@withContext Result.failure()
                    }
                }
            } finally {
                connection.disconnect()
            }
            if (temp.length() == 0L) return@withContext Result.retry()
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            write(progressKey, AudioDownloadState.COMPLETED, 100, target.length(), target.length())
            Result.success()
        } catch (_: Exception) {
            write(progressKey, AudioDownloadState.FAILED, 0, temp.takeIf { it.exists() }?.length() ?: 0L, 0L)
            Result.retry()
        }
    }

    private fun open(url: String, fromByte: Long): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "QuranPremium/1.6")
        if (fromByte > 0L) setRequestProperty("Range", "bytes=$fromByte-")
    }

    private fun percent(done: Long, total: Long): Int = if (total <= 0L) 0 else ((done * 100L) / total).toInt().coerceIn(0, 100)

    private fun write(key: String, state: AudioDownloadState, percent: Int, done: Long, total: Long) {
        prefs.edit()
            .putString("$key.state", state.name)
            .putInt("$key.percent", percent)
            .putLong("$key.done", done)
            .putLong("$key.total", total)
            .apply()
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_PATH = "path"
        const val KEY_PROGRESS_KEY = "progress_key"
    }
}
