package com.example.features.player.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.core.local.studioLibraryDataStore
import com.example.core.model.TrackLyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LyricsRepository(context: Context) {
    private val dataStore = context.applicationContext.studioLibraryDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val lyricsMapKey = stringPreferencesKey("track_lyrics_map_json")

    fun observeTrackLyrics(trackId: Int): Flow<TrackLyrics?> =
        dataStore.data.map { prefs ->
            decodeMap(prefs[lyricsMapKey])[trackId]
        }

    suspend fun saveTrackLyrics(trackId: Int, lrcContent: String): Result<Unit> {
        val cleanContent = lrcContent.trim().take(15000)
        if (cleanContent.isEmpty()) {
            return Result.failure(IllegalArgumentException("Lyrics content cannot be empty"))
        }
        return try {
            val entry = TrackLyrics(
                trackNumber = trackId.toLong(),
                lrcContent = cleanContent,
                updatedAtMs = System.currentTimeMillis()
            )
            dataStore.edit { prefs ->
                val updated = decodeMap(prefs[lyricsMapKey]).toMutableMap()
                updated[trackId] = entry
                prefs[lyricsMapKey] = json.encodeToString(updated)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeMap(raw: String?): Map<Int, TrackLyrics> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<Int, TrackLyrics>>(raw) }.getOrDefault(emptyMap())
    }
}
