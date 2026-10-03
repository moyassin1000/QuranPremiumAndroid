package com.qurankareem.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qurankareem.core.quran.QuranMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readingDataStore by preferencesDataStore(name = "reading_state")

data class ReadingState(
    val lastPage: Int = 1,
    val bookmarkedPages: Set<Int> = emptySet(),
    val favoritePages: Set<Int> = emptySet(),
    val bookmarkedAyahs: Set<String> = emptySet(),
    val favoriteAyahs: Set<String> = emptySet(),
)

class ReadingStore(private val context: Context) {
    private val lastPageKey = intPreferencesKey("last_page")
    private val bookmarksKey = stringSetPreferencesKey("bookmarked_pages")
    private val favoritesKey = stringSetPreferencesKey("favorite_pages")
    private val ayahBookmarksKey = stringSetPreferencesKey("bookmarked_ayahs_v2")
    private val ayahFavoritesKey = stringSetPreferencesKey("favorite_ayahs_v2")

    val state: Flow<ReadingState> = context.readingDataStore.data.map { prefs ->
        ReadingState(
            lastPage = (prefs[lastPageKey] ?: 1).coerceIn(1, QuranMetadata.TOTAL_PAGES),
            bookmarkedPages = prefs[bookmarksKey].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
            favoritePages = prefs[favoritesKey].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
            bookmarkedAyahs = prefs[ayahBookmarksKey].orEmpty().filter { QuranMetadata.ayahRef(it) != null }.toSet(),
            favoriteAyahs = prefs[ayahFavoritesKey].orEmpty().filter { QuranMetadata.ayahRef(it) != null }.toSet(),
        )
    }

    suspend fun setLastPage(page: Int) {
        context.readingDataStore.edit { it[lastPageKey] = page.coerceIn(1, QuranMetadata.TOTAL_PAGES) }
    }

    suspend fun toggleBookmark(page: Int) = togglePageSet(bookmarksKey, page)

    suspend fun toggleFavorite(page: Int) = togglePageSet(favoritesKey, page)

    suspend fun toggleAyahBookmark(surah: Int, ayah: Int) = toggleAyahSet(ayahBookmarksKey, surah, ayah)

    suspend fun toggleAyahFavorite(surah: Int, ayah: Int) = toggleAyahSet(ayahFavoritesKey, surah, ayah)

    suspend fun replaceState(state: ReadingState) {
        context.readingDataStore.edit { prefs ->
            prefs[lastPageKey] = state.lastPage.coerceIn(1, QuranMetadata.TOTAL_PAGES)
            prefs[bookmarksKey] = state.bookmarkedPages.filter { it in 1..QuranMetadata.TOTAL_PAGES }.map(Int::toString).toSet()
            prefs[favoritesKey] = state.favoritePages.filter { it in 1..QuranMetadata.TOTAL_PAGES }.map(Int::toString).toSet()
            prefs[ayahBookmarksKey] = state.bookmarkedAyahs.filter { QuranMetadata.ayahRef(it) != null }.toSet()
            prefs[ayahFavoritesKey] = state.favoriteAyahs.filter { QuranMetadata.ayahRef(it) != null }.toSet()
        }
    }

    private suspend fun togglePageSet(key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>, page: Int) {
        context.readingDataStore.edit { prefs ->
            val value = page.coerceIn(1, QuranMetadata.TOTAL_PAGES).toString()
            val current = prefs[key].orEmpty().toMutableSet()
            if (!current.add(value)) current.remove(value)
            prefs[key] = current
        }
    }

    private suspend fun toggleAyahSet(
        key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>,
        surah: Int,
        ayah: Int,
    ) {
        val safeSurah = surah.coerceIn(1, QuranMetadata.TOTAL_SURAHS)
        val safeAyah = ayah.coerceIn(1, QuranMetadata.surah(safeSurah).ayahCount)
        val value = QuranMetadata.ayahKey(safeSurah, safeAyah)
        context.readingDataStore.edit { prefs ->
            val current = prefs[key].orEmpty().toMutableSet()
            if (!current.add(value)) current.remove(value)
            prefs[key] = current
        }
    }
}
