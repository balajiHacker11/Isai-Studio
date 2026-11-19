package com.example.core.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.LinkedAudioEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.isaiDataStore: DataStore<Preferences> by preferencesDataStore(name = "isai_studio_local")

@Serializable
private data class StoredAudioRecord(
    val trackId: Int,
    val uriString: String,
    val displayName: String,
    val durationMs: Long = 0L
)

class LocalAudioStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val INTRO_COMPLETED = booleanPreferencesKey("intro_completed")
        val LINKED_AUDIO_JSON = stringPreferencesKey("linked_audio_json")
    }

    val introCompletedFlow: Flow<Boolean> = context.isaiDataStore.data.map { prefs ->
        prefs[Keys.INTRO_COMPLETED] ?: false
    }

    val linkedAudioFlow: Flow<Map<Int, LinkedAudioEntry>> = context.isaiDataStore.data.map { prefs ->
        decodeMap(prefs[Keys.LINKED_AUDIO_JSON])
    }

    suspend fun setIntroCompleted(completed: Boolean) {
        context.isaiDataStore.edit { prefs ->
            prefs[Keys.INTRO_COMPLETED] = completed
        }
    }

    suspend fun linkTrackAudio(trackId: Int, uri: Uri): LinkedAudioEntry {
        tryTakePersistablePermission(uri)
        val displayName = resolveDisplayName(uri)
        val entry = LinkedAudioEntry(
            trackId = trackId,
            uriString = uri.toString(),
            displayName = displayName
        )
        context.isaiDataStore.edit { prefs ->
            val current = decodeMap(prefs[Keys.LINKED_AUDIO_JSON]).toMutableMap()
            current[trackId] = entry
            prefs[Keys.LINKED_AUDIO_JSON] = encodeMap(current)
        }
        return entry
    }

    suspend fun linkMultipleTracks(matches: Map<Int, Uri>): Int {
        if (matches.isEmpty()) return 0
        val newEntries = matches.mapValues { (trackId, uri) ->
            tryTakePersistablePermission(uri)
            LinkedAudioEntry(
                trackId = trackId,
                uriString = uri.toString(),
                displayName = resolveDisplayName(uri)
            )
        }
        context.isaiDataStore.edit { prefs ->
            val current = decodeMap(prefs[Keys.LINKED_AUDIO_JSON]).toMutableMap()
            current.putAll(newEntries)
            prefs[Keys.LINKED_AUDIO_JSON] = encodeMap(current)
        }
        return newEntries.size
    }

    suspend fun unlinkTrackAudio(trackId: Int) {
        context.isaiDataStore.edit { prefs ->
            val current = decodeMap(prefs[Keys.LINKED_AUDIO_JSON]).toMutableMap()
            current.remove(trackId)
            prefs[Keys.LINKED_AUDIO_JSON] = encodeMap(current)
        }
    }

    suspend fun clearAllLinkedAudio() {
        context.isaiDataStore.edit { prefs ->
            prefs.remove(Keys.LINKED_AUDIO_JSON)
        }
    }

    fun resolveDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return@use cursor.getString(idx)
                    }
                    null
                } ?: uri.lastPathSegment ?: "Local Audio"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Local Audio"
        }
    }

    private fun tryTakePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some temporary document providers only grant transient read access.
        }
    }

    private fun decodeMap(raw: String?): Map<Int, LinkedAudioEntry> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val list = json.decodeFromString<List<StoredAudioRecord>>(raw)
            list.associate { it.trackId to LinkedAudioEntry(it.trackId, it.uriString, it.displayName, it.durationMs) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun encodeMap(map: Map<Int, LinkedAudioEntry>): String {
        val records = map.values.map {
            StoredAudioRecord(it.trackId, it.uriString, it.displayName, it.durationMs)
        }
        return json.encodeToString(records)
    }
}
