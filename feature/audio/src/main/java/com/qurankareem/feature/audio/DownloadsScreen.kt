package com.qurankareem.feature.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qurankareem.core.audio.AudioDownloadProgress
import com.qurankareem.core.audio.AudioDownloadState
import com.qurankareem.core.audio.AudioDownloadStore
import com.qurankareem.core.audio.DownloadedAudio
import com.qurankareem.core.quran.QuranMetadata
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val store = remember { AudioDownloadStore(context.applicationContext) }
    var items by remember { mutableStateOf(store.downloaded()) }
    var active by remember { mutableStateOf(store.activeProgress()) }

    fun refresh() {
        items = store.downloaded()
        active = store.activeProgress()
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(1_000)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("إدارة التنزيلات") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("المحتوى المحمل", style = MaterialTheme.typography.titleLarge)
                        Text("${items.size} سورة • ${formatBytes(items.sumOf { it.sizeBytes })}")
                        if (active.isNotEmpty()) Text("${active.size} عملية تنزيل نشطة أو معلقة", style = MaterialTheme.typography.bodySmall)
                    }
                    if (items.isNotEmpty()) {
                        Button(onClick = { store.deleteAll(); refresh() }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                            Text("حذف الكل", modifier = Modifier.padding(horizontal = 6.dp))
                        }
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (active.isNotEmpty()) {
                    item { Text("التنزيلات الحالية", style = MaterialTheme.typography.titleMedium) }
                    items(active, key = { "active-${it.reciterId}-${it.moshafId}-${it.surah}" }) { progress ->
                        ActiveDownloadRow(
                            progress = progress,
                            onCancel = { store.cancel(progress.reciterId, progress.moshafId, progress.surah); refresh() },
                        )
                    }
                }

                item { Text("الملفات الجاهزة", style = MaterialTheme.typography.titleMedium) }
                if (items.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "لا توجد سور محملة بعد. يمكنك تنزيل أي سورة من شاشة الاستماع لتشغيلها بدون إنترنت.",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                } else {
                    items(items, key = { it.file.absolutePath }) { item ->
                        DownloadRow(item = item, onDelete = { store.delete(item); refresh() })
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadRow(progress: AudioDownloadProgress, onCancel: () -> Unit) {
    val surah = QuranMetadata.surah(progress.surah)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleMedium)
                    Text(downloadStateLabel(progress.state), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onCancel) { Icon(Icons.Outlined.Cancel, contentDescription = "إلغاء") }
            }
            if (progress.totalBytes > 0L) {
                LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Text("${progress.percent}% • ${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}", style = MaterialTheme.typography.bodySmall)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (progress.downloadedBytes > 0L) Text("تم تنزيل ${formatBytes(progress.downloadedBytes)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadedAudio, onDelete: () -> Unit) {
    val surah = QuranMetadata.surahs.firstOrNull { it.number == item.surah }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("سورة ${surah?.nameArabic ?: item.surah}", style = MaterialTheme.typography.titleMedium)
                Text("${formatBytes(item.sizeBytes)} • قارئ #${item.reciterId}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "حذف") }
        }
    }
}

private fun downloadStateLabel(state: AudioDownloadState): String = when (state) {
    AudioDownloadState.QUEUED -> "في انتظار الشبكة المناسبة"
    AudioDownloadState.DOWNLOADING -> "جارٍ التنزيل"
    AudioDownloadState.FAILED -> "تعذر التنزيل — سيحاول النظام مجددًا"
    AudioDownloadState.COMPLETED -> "مكتمل"
    AudioDownloadState.NONE -> ""
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
