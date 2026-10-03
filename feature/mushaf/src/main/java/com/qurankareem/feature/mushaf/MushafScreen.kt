package com.qurankareem.feature.mushaf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qurankareem.core.design.PremiumCard
import com.qurankareem.core.quran.QuranMetadata
import com.qurankareem.core.quran.QuranSearchHit
import com.qurankareem.core.quran.QuranTextRepository
import kotlinx.coroutines.launch

@Composable
fun MushafScreen(
    lastPage: Int,
    bookmarkedAyahs: Set<String>,
    favoriteAyahs: Set<String>,
    onContinueReading: () -> Unit,
    onOpenPage: (Int) -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenJuz: (Int) -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { QuranTextRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var ayahQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<QuranSearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var offlineReady by remember { mutableStateOf(repository.offlinePackReady()) }
    var offlineDownloading by remember { mutableStateOf(false) }
    var offlineMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("المصحف الشريف", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "قراءة مرتبة بالسور والأجزاء وصفحات مصحف المدينة",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            Spacer(Modifier.height(12.dp))
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("آخر قراءة", fontWeight = FontWeight.SemiBold)
                        Text("الصفحة ${arabicNumber(lastPage)}", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedButton(onClick = onContinueReading) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = null)
                        Text(" متابعة")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OfflinePackCard(
                ready = offlineReady,
                downloading = offlineDownloading,
                message = offlineMessage,
                onDownload = {
                    offlineDownloading = true
                    offlineMessage = null
                    scope.launch {
                        runCatching { repository.downloadOfflinePack(force = false) }
                            .onSuccess {
                                offlineReady = true
                                offlineMessage = "تم تجهيز القرآن النصي كاملًا للعمل دون إنترنت"
                            }
                            .onFailure { offlineMessage = it.message ?: "تعذر تنزيل الحزمة" }
                        offlineDownloading = false
                    }
                },
            )
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            listOf("السور", "الأجزاء", "الصفحات", "بحث", "المحفوظات").forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        when (selectedTab) {
            0 -> {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    singleLine = true,
                    label = { Text("ابحث باسم السورة") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                )
                val filtered = remember(query) {
                    QuranMetadata.surahs.filter { it.nameArabic.contains(query.trim(), ignoreCase = true) }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.number }) { surah ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .clickable { onOpenSurah(surah.number) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AssistChip(onClick = { onOpenSurah(surah.number) }, label = { Text(arabicNumber(surah.number)) })
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text("سورة ${surah.nameArabic}", fontWeight = FontWeight.Bold)
                                        Text("${arabicNumber(surah.ayahCount)} آية", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                                    }
                                }
                                Text("فتح", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            1 -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                items((1..QuranMetadata.TOTAL_JUZ).toList()) { juz ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onOpenJuz(juz) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("الجزء ${arabicNumber(juz)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("قراءة الجزء", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            2 -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((1..QuranMetadata.TOTAL_PAGES).toList()) { page ->
                    Card(
                        modifier = Modifier.height(72.dp).clickable { onOpenPage(page) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (page == lastPage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(arabicNumber(page), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (page == lastPage) Text("آخر قراءة", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            3 -> QuranSearchTab(
                query = ayahQuery,
                onQueryChange = { ayahQuery = it },
                results = searchResults,
                searching = searching,
                error = searchError,
                offlineReady = offlineReady,
                onSearch = {
                    val value = ayahQuery.trim()
                    if (value.length < 2) {
                        searchError = "اكتب حرفين على الأقل"
                    } else {
                        searching = true
                        searchError = null
                        scope.launch {
                            runCatching { repository.search(value, maxResults = 120) }
                                .onSuccess {
                                    searchResults = it
                                    offlineReady = repository.offlinePackReady()
                                    if (it.isEmpty()) searchError = "لم يتم العثور على نتائج"
                                }
                                .onFailure { searchError = it.message ?: "تعذر البحث الآن" }
                            searching = false
                        }
                    }
                },
                onOpenAyah = onOpenAyah,
            )

            else -> SavedAyahsTab(
                bookmarkedAyahs = bookmarkedAyahs,
                favoriteAyahs = favoriteAyahs,
                onOpenAyah = onOpenAyah,
            )
        }
    }
}

@Composable
private fun OfflinePackCard(
    ready: Boolean,
    downloading: Boolean,
    message: String?,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (ready) Icons.Outlined.CloudDone else Icons.Outlined.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(if (ready) "القرآن النصي جاهز Offline" else "تنزيل حزمة القراءة Offline", fontWeight = FontWeight.Bold)
                Text(
                    message ?: if (ready) "يمكن فتح السور والأجزاء والصفحات والبحث بدون إنترنت." else "تنزيل واحد يجهز النص الكامل للقراءة والبحث دون اتصال.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!ready) {
                Button(onClick = onDownload, enabled = !downloading) {
                    if (downloading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else Text("تنزيل")
                }
            }
        }
    }
}

@Composable
private fun QuranSearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<QuranSearchHit>,
    searching: Boolean,
    error: String?,
    offlineReady: Boolean,
    onSearch: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true,
            label = { Text("ابحث داخل آيات القرآن") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                OutlinedButton(onClick = onSearch, enabled = !searching) { Text("بحث") }
            },
        )
        Text(
            if (offlineReady) "البحث محلي على الجهاز" else "عند أول بحث سيتم تنزيل حزمة النص الكامل ثم يصبح البحث Offline",
            modifier = Modifier.padding(horizontal = 18.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
        if (searching) {
            Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null) {
            Text(error, modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.ayah.key }) { hit ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenAyah(hit.ayah.surah, hit.ayah.ayah) },
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سورة ${hit.surahName} • آية ${arabicNumber(hit.ayah.ayah)}", fontWeight = FontWeight.Bold)
                            Text("ص ${arabicNumber(hit.page)}", color = MaterialTheme.colorScheme.primary)
                        }
                        Text(hit.ayah.text, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Right)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAyahsTab(
    bookmarkedAyahs: Set<String>,
    favoriteAyahs: Set<String>,
    onOpenAyah: (Int, Int) -> Unit,
) {
    val keys = remember(bookmarkedAyahs, favoriteAyahs) {
        (bookmarkedAyahs + favoriteAyahs)
            .mapNotNull { QuranMetadata.ayahRef(it) }
            .distinct()
            .sortedBy { QuranMetadata.ordinal(it.surah, it.ayah) }
    }
    if (keys.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد آيات محفوظة بعد\nافتح أي آية وأضف علامة أو مفضلة.", textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(keys, key = { "${it.surah}:${it.ayah}" }) { ref ->
            val key = QuranMetadata.ayahKey(ref.surah, ref.ayah)
            Card(Modifier.fillMaxWidth().clickable { onOpenAyah(ref.surah, ref.ayah) }) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("سورة ${QuranMetadata.surah(ref.surah).nameArabic}", fontWeight = FontWeight.Bold)
                        Text("الآية ${arabicNumber(ref.ayah)} • الصفحة ${arabicNumber(QuranMetadata.pageForAyah(ref.surah, ref.ayah))}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (key in bookmarkedAyahs) Icon(Icons.Outlined.Bookmark, contentDescription = "علامة", tint = MaterialTheme.colorScheme.primary)
                        if (key in favoriteAyahs) Icon(Icons.Outlined.Favorite, contentDescription = "مفضلة", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

internal fun arabicNumber(value: Int): String = value.toString().map {
    when (it) {
        '0' -> '٠'; '1' -> '١'; '2' -> '٢'; '3' -> '٣'; '4' -> '٤'
        '5' -> '٥'; '6' -> '٦'; '7' -> '٧'; '8' -> '٨'; '9' -> '٩'
        else -> it
    }
}.joinToString("")
