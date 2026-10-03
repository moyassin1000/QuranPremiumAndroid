package com.qurankareem.feature.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.qurankareem.core.audio.AudioCatalogRepository
import com.qurankareem.core.audio.AudioDownloadStore
import com.qurankareem.core.audio.QuranReciter
import com.qurankareem.core.quran.QuranMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    currentMediaId: String?,
    isPlaying: Boolean,
    playbackSpeed: Float,
    repeatOne: Boolean,
    sleepTimerLabel: String,
    defaultReciterId: Int,
    defaultMoshafId: Int,
    wifiOnlyDownloads: Boolean,
    onPlay: (QuranReciter, Int, String) -> Unit,
    onPauseResume: () -> Unit,
    onPlaybackSpeed: (Float) -> Unit,
    onRepeatOne: (Boolean) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onDefaultReciter: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val catalog = remember { AudioCatalogRepository(context.applicationContext) }
    val downloads = remember { AudioDownloadStore(context.applicationContext) }
    var reciters by remember { mutableStateOf<List<QuranReciter>>(emptyList()) }
    var selected by remember { mutableStateOf<QuranReciter?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableStateOf(0) }
    var speedMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }

    suspend fun load(force: Boolean = false) {
        loading = true
        error = null
        runCatching { catalog.reciters(force) }
            .onSuccess { reciters = it }
            .onFailure { error = it.message ?: "تعذر تحميل القراء" }
        loading = false
    }

    LaunchedEffect(refreshToken) { load(refreshToken > 0) }
    LaunchedEffect(reciters, defaultReciterId, defaultMoshafId) {
        if (reciters.isNotEmpty()) {
            val preferred = reciters.firstOrNull { it.id == defaultReciterId && it.moshaf.id == defaultMoshafId }
            val complete = reciters.firstOrNull { it.moshaf.surahNumbers.size == 114 }
            selected = preferred ?: selected?.takeIf { current -> reciters.any { it.id == current.id && it.moshaf.id == current.moshaf.id } } ?: complete ?: reciters.first()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("الاستماع للقرآن") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))

            if (currentMediaId != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("المشغل", style = MaterialTheme.typography.titleMedium)
                                Text(if (isPlaying) "التلاوة تعمل الآن" else "التلاوة متوقفة مؤقتًا", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = onPauseResume) {
                                Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.weight(1f)) {
                                OutlinedButton(onClick = { speedMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("السرعة ${formatSpeed(playbackSpeed)}")
                                }
                                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text(formatSpeed(speed)) },
                                            onClick = { onPlaybackSpeed(speed); speedMenu = false },
                                        )
                                    }
                                }
                            }
                            OutlinedButton(onClick = { onRepeatOne(!repeatOne) }, modifier = Modifier.weight(1f)) {
                                Text(if (repeatOne) "التكرار: مفعل" else "التكرار: متوقف")
                            }
                        }
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { sleepMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("مؤقت النوم: $sleepTimerLabel")
                            }
                            DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                                listOf(0, 5, 10, 15, 30, 45, 60).forEach { minutes ->
                                    DropdownMenuItem(
                                        text = { Text(if (minutes == 0) "إيقاف المؤقت" else "$minutes دقيقة") },
                                        onClick = { onSleepTimer(minutes); sleepMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Text("اختر القارئ", style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxWidth()) {
                Button(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Headphones, null)
                    Text(selected?.name ?: "اختيار قارئ", modifier = Modifier.padding(horizontal = 8.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    reciters.take(100).forEach { reciter ->
                        DropdownMenuItem(
                            text = { Text(reciter.name) },
                            onClick = {
                                selected = reciter
                                onDefaultReciter(reciter.id, reciter.moshaf.id)
                                menuOpen = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ابحث عن سورة") },
                singleLine = true,
            )
            if (wifiOnlyDownloads) {
                Text("التنزيل مضبوط على Wi-Fi فقط", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            if (error != null && reciters.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!)
                    Button(onClick = { refreshToken++ }, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Outlined.Refresh, null); Text("حاول مرة أخرى")
                    }
                }
                return@Column
            }
            val reciter = selected
            val visible = QuranMetadata.surahs.filter { s ->
                (query.isBlank() || s.nameArabic.contains(query.trim())) && (reciter?.moshaf?.surahNumbers?.contains(s.number) != false)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(visible, key = { it.number }) { surah ->
                    val mediaId = reciter?.let { "${it.id}:${it.moshaf.id}:${surah.number}" }
                    val active = mediaId != null && mediaId == currentMediaId
                    val downloaded = reciter?.let { downloads.isDownloaded(it, surah.number) } == true
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (reciter != null) onPlay(reciter, surah.number, surah.nameArabic)
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${surah.number}", style = MaterialTheme.typography.labelLarge)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleMedium)
                            Text(reciter?.name ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            if (reciter != null && !downloaded) downloads.enqueue(reciter, surah.number, wifiOnlyDownloads)
                        }) {
                            Icon(if (downloaded) Icons.Outlined.DownloadDone else Icons.Outlined.Download, contentDescription = if (downloaded) "تم التنزيل" else "تنزيل")
                        }
                        IconButton(onClick = {
                            if (active) onPauseResume() else if (reciter != null) onPlay(reciter, surah.number, surah.nameArabic)
                        }) {
                            Icon(if (active && isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

private fun formatSpeed(speed: Float): String = if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"
