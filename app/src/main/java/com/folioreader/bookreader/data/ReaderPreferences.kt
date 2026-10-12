package com.folioreader.bookreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_settings")

enum class ReaderTheme(val title: String) {
    PAPER("Warm Paper"),
    LIGHT("Daylight"),
    DARK("Midnight")
}

enum class BookLayoutMode(val title: String) {
    AUTO("Auto"),
    SINGLE("Single Page"),
    DUAL("Facing Pages")
}

data class RecentBook(
    val uriString: String,
    val title: String,
    val lastPage: Int,
    val totalPages: Int,
    val lastOpenedTime: Long
)

data class ReaderPreferencesState(
    val theme: ReaderTheme = ReaderTheme.PAPER,
    val layoutMode: BookLayoutMode = BookLayoutMode.AUTO,
    val curlAnimationEnabled: Boolean = true,
    val recentBooks: List<RecentBook> = emptyList()
)

class ReaderPreferences(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("reader_theme")
        val LAYOUT_MODE = stringPreferencesKey("reader_layout_mode")
        val CURL_ANIMATION = booleanPreferencesKey("reader_curl_animation")
        val RECENT_BOOKS_JSON = stringPreferencesKey("recent_books_json")
        val LAST_PAGE_PREFIX = "last_page_"
    }

    val preferencesFlow: Flow<ReaderPreferencesState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME] ?: ReaderTheme.PAPER.name
        val theme = try { ReaderTheme.valueOf(themeStr) } catch (_: Exception) { ReaderTheme.PAPER }

        val layoutStr = prefs[Keys.LAYOUT_MODE] ?: BookLayoutMode.AUTO.name
        val layout = try { BookLayoutMode.valueOf(layoutStr) } catch (_: Exception) { BookLayoutMode.AUTO }

        val curlEnabled = prefs[Keys.CURL_ANIMATION] ?: true

        val recentsJson = prefs[Keys.RECENT_BOOKS_JSON] ?: "[]"
        val recentBooks = parseRecents(recentsJson)

        ReaderPreferencesState(
            theme = theme,
            layoutMode = layout,
            curlAnimationEnabled = curlEnabled,
            recentBooks = recentBooks
        )
    }

    suspend fun setTheme(theme: ReaderTheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.name
        }
    }

    suspend fun setLayoutMode(mode: BookLayoutMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAYOUT_MODE] = mode.name
        }
    }

    suspend fun setCurlAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURL_ANIMATION] = enabled
        }
    }

    suspend fun saveDocumentProgress(uriString: String, title: String, page: Int, totalPages: Int) {
        context.dataStore.edit { prefs ->
            // Save specific page
            val pageKey = stringPreferencesKey(Keys.LAST_PAGE_PREFIX + uriString.hashCode())
            prefs[pageKey] = page.toString()

            // Update recent books
            val currentRecentsJson = prefs[Keys.RECENT_BOOKS_JSON] ?: "[]"
            val recents = parseRecents(currentRecentsJson).toMutableList()

            // Remove existing entry if present
            recents.removeAll { it.uriString == uriString }

            // Add to head
            recents.add(
                0,
                RecentBook(
                    uriString = uriString,
                    title = title,
                    lastPage = page,
                    totalPages = totalPages,
                    lastOpenedTime = System.currentTimeMillis()
                )
            )

            // Limit to top 20 recent books
            val trimmed = recents.take(20)
            prefs[Keys.RECENT_BOOKS_JSON] = serializeRecents(trimmed)
        }
    }

    suspend fun getLastReadPage(uriString: String): Int {
        var page = 0
        context.dataStore.edit { prefs ->
            val pageKey = stringPreferencesKey(Keys.LAST_PAGE_PREFIX + uriString.hashCode())
            page = prefs[pageKey]?.toIntOrNull() ?: 0
        }
        return page
    }

    suspend fun removeRecentBook(uriString: String) {
        context.dataStore.edit { prefs ->
            val currentRecentsJson = prefs[Keys.RECENT_BOOKS_JSON] ?: "[]"
            val recents = parseRecents(currentRecentsJson).filter { it.uriString != uriString }
            prefs[Keys.RECENT_BOOKS_JSON] = serializeRecents(recents)
        }
    }

    private fun parseRecents(jsonString: String): List<RecentBook> {
        val list = mutableListOf<RecentBook>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentBook(
                        uriString = obj.optString("uri", ""),
                        title = obj.optString("title", "Untitled"),
                        lastPage = obj.optInt("lastPage", 0),
                        totalPages = obj.optInt("totalPages", 1),
                        lastOpenedTime = obj.optLong("time", 0L)
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }

    private fun serializeRecents(list: List<RecentBook>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("uri", item.uriString)
                put("title", item.title)
                put("lastPage", item.lastPage)
                put("totalPages", item.totalPages)
                put("time", item.lastOpenedTime)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
