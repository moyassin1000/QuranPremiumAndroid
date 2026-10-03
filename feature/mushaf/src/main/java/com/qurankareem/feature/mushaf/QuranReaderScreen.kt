package com.qurankareem.feature.mushaf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qurankareem.core.design.PremiumCard
import com.qurankareem.core.quran.QuranAyah
import com.qurankareem.core.quran.QuranMetadata
import com.qurankareem.core.quran.QuranTextRepository


enum class ReaderKind { PAGE, SURAH, JUZ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    kind: ReaderKind,
    number: Int,
    bookmarkedPages: Set<Int>,
    favoritePages: Set<Int>,
    bookmarkedAyahs: Set<String>,
    favoriteAyahs: Set<String>,
    initialAyah: Int = 0,
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit,
    onPageViewed: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onToggleAyahBookmark: (Int, Int) -> Unit,
    onToggleAyahFavorite: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { QuranTextRepository(context.applicationContext) }
    val listState = rememberLazyListState()
    var loadKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var ayahs by remember { mutableStateOf<List<QuranAyah>>(emptyList()) }
    var fromCache by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(29f) }
    var showSettings by remember { mutableStateOf(false) }
    var focusMode by remember { mutableStateOf(false) }

    LaunchedEffect(kind, number, loadKey) {
        loading = true
        error = null
        runCatching {
            when (kind) {
                ReaderKind.PAGE -> repository.page(number)
                ReaderKind.SURAH -> repository.surah(number)
                ReaderKind.JUZ -> repository.juz(number)
            }
        }.onSuccess {
            ayahs = it.ayahs
            fromCache = it.fromCache
            if (kind == ReaderKind.PAGE) onPageViewed(number)
        }.onFailure {
            error = it.message ?: "تعذر تحميل النص القرآني"
        }
        loading = false
    }

    LaunchedEffect(ayahs, initialAyah) {
        if (initialAyah > 0 && ayahs.isNotEmpty()) {
            val index = ayahs.indexOfFirst { it.ayah == initialAyah && (kind != ReaderKind.SURAH || it.surah == number) }
            if (index >= 0) listState.scrollToItem(index + 1)
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                Text("إعدادات القراءة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("حجم النص: ${fontSize.toInt()}", modifier = Modifier.padding(top = 18.dp))
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 22f..44f,
                    steps = 10,
                )
                Text("خط القراءة: عربي Serif عالي الوضوح", fontWeight = FontWeight.SemiBold)
                Text(
                    "تم تحسين التباعد وارتفاع السطر لقراءة أطول وأكثر راحة. إعدادات الورق والسطوع ستبقى قابلة للتوسع.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!focusMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(readerTitle(kind, number), fontWeight = FontWeight.Bold)
                    if (kind == ReaderKind.PAGE) Text("من ٦٠٤", style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Outlined.FormatSize, contentDescription = "حجم الخط") }
                    IconButton(onClick = { focusMode = true }) { Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "وضع التركيز") }
                }
            }

            if (kind == ReaderKind.PAGE) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { onToggleBookmark(number) }, modifier = Modifier.weight(1f)) {
                        Icon(
                            if (number in bookmarkedPages) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                        )
                        Text(if (number in bookmarkedPages) " محفوظة" else " علامة صفحة")
                    }
                    OutlinedButton(onClick = { onToggleFavorite(number) }, modifier = Modifier.weight(1f)) {
                        Icon(
                            if (number in favoritePages) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                        )
                        Text(if (number in favoritePages) " مفضلة" else " مفضلة صفحة")
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AssistChip(
                    onClick = { focusMode = false },
                    label = { Text("إظهار الأدوات") },
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("جاري تحميل النص الموثق…", modifier = Modifier.padding(top = 12.dp))
                }
            }

            error != null -> Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(22.dp), contentAlignment = Alignment.Center) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("تعذر فتح القراءة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${error}\n\nيمكنك تنزيل حزمة القرآن Offline من شاشة المصحف لتعمل كل الصفحات والسور والأجزاء دون إنترنت.",
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Button(onClick = { loadKey += 1 }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Text(" إعادة المحاولة")
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("حفص عن عاصم", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(if (fromCache) "من التخزين المحلي" else "تم حفظها على الجهاز", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    items(ayahs, key = { it.key }) { ayah ->
                        AyahRow(
                            ayah = ayah,
                            fontSize = fontSize,
                            isTarget = initialAyah > 0 && ayah.ayah == initialAyah && (kind != ReaderKind.SURAH || ayah.surah == number),
                            bookmarked = ayah.key in bookmarkedAyahs,
                            favorite = ayah.key in favoriteAyahs,
                            showSurahLabel = kind != ReaderKind.SURAH,
                            onToggleBookmark = { onToggleAyahBookmark(ayah.surah, ayah.ayah) },
                            onToggleFavorite = { onToggleAyahFavorite(ayah.surah, ayah.ayah) },
                        )
                    }
                    item {
                        Text(
                            "مصدر النص: ${QuranTextRepository.SOURCE_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (!focusMode && kind == ReaderKind.PAGE && !loading && error == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onOpenPage((number - 1).coerceAtLeast(1)) },
                    enabled = number > 1,
                    modifier = Modifier.weight(1f),
                ) { Text("الصفحة السابقة") }
                Button(
                    onClick = { onOpenPage((number + 1).coerceAtMost(QuranMetadata.TOTAL_PAGES)) },
                    enabled = number < QuranMetadata.TOTAL_PAGES,
                    modifier = Modifier.weight(1f),
                ) { Text("الصفحة التالية") }
            }
        }
    }
}

@Composable
private fun AyahRow(
    ayah: QuranAyah,
    fontSize: Float,
    isTarget: Boolean,
    bookmarked: Boolean,
    favorite: Boolean,
    showSurahLabel: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTarget) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = {}, label = { Text(arabicNumber(ayah.ayah)) })
                    if (showSurahLabel) {
                        Text(
                            "سورة ${QuranMetadata.surah(ayah.surah).nameArabic}",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Row {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "علامة للآية",
                            tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "إضافة للمفضلة",
                            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = ayah.text,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                fontFamily = FontFamily.Serif,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.9f).sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Right,
            )
        }
    }
}

private fun readerTitle(kind: ReaderKind, number: Int): String = when (kind) {
    ReaderKind.PAGE -> "الصفحة ${arabicNumber(number)}"
    ReaderKind.SURAH -> "سورة ${QuranMetadata.surah(number).nameArabic}"
    ReaderKind.JUZ -> "الجزء ${arabicNumber(number)}"
}
