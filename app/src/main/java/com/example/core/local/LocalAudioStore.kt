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
import com.example.R
import com.example.core.domain.AutoMatchAudioFilesUseCase
import com.example.core.model.CustomDeviceTrack
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.Track
import com.example.core.model.TrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    @Volatile
    private var cachedCustomTracks: Map<Int, Track> = emptyMap()

    private object Keys {
        val INTRO_COMPLETED = booleanPreferencesKey("intro_completed")
        val LINKED_AUDIO_JSON = stringPreferencesKey("linked_audio_json")
        val CUSTOM_TRACKS_JSON = stringPreferencesKey("custom_device_tracks_json")
    }

    val introCompletedFlow: Flow<Boolean> = context.isaiDataStore.data.map { prefs ->
        prefs[Keys.INTRO_COMPLETED] ?: false
    }

    val customTracksFlow: Flow<List<Track>> = context.isaiDataStore.data.map { prefs ->
        val tracks = decodeCustomTracks(prefs[Keys.CUSTOM_TRACKS_JSON]).map { it.toTrack() }
        cachedCustomTracks = tracks.associateBy { it.id }
        tracks
    }

    val linkedAudioFlow: Flow<Map<Int, LinkedAudioEntry>> = context.isaiDataStore.data.map { prefs ->
        val linked = decodeMap(prefs[Keys.LINKED_AUDIO_JSON]).toMutableMap()
        decodeCustomTracks(prefs[Keys.CUSTOM_TRACKS_JSON]).forEach { custom ->
            linked[custom.id] = LinkedAudioEntry(
                trackId = custom.id,
                uriString = custom.uriString,
                displayName = custom.displayName,
                durationMs = custom.durationMs
            )
        }
        linked
    }

    fun customTrackById(id: Int): Track? = cachedCustomTracks[id]

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

    suspend fun importMusicFolder(
        treeUri: Uri,
        catalogTracks: List<Track>,
        autoMatchUseCase: AutoMatchAudioFilesUseCase
    ): AudioImportSummary = withContext(Dispatchers.IO) {
        val discovered = AudioTreeScanner.scanFolder(context, treeUri)
        val candidates = discovered.map { file ->
            AudioTreeScanner.buildCandidate(context, file.uri, file.displayName)
        }
        importAudioCandidates(candidates, catalogTracks, autoMatchUseCase)
    }

    suspend fun importMultipleAudioUris(
        uris: List<Uri>,
        catalogTracks: List<Track>,
        autoMatchUseCase: AutoMatchAudioFilesUseCase
    ): AudioImportSummary = withContext(Dispatchers.IO) {
        val candidates = uris.map { uri ->
            AudioTreeScanner.buildCandidate(context, uri)
        }
        importAudioCandidates(candidates, catalogTracks, autoMatchUseCase)
    }

    suspend fun importAudioCandidates(
        candidates: List<AudioImportCandidate>,
        catalogTracks: List<Track>,
        autoMatchUseCase: AutoMatchAudioFilesUseCase
    ): AudioImportSummary {
        if (candidates.isEmpty()) {
            return AudioImportSummary(0, 0, 0, emptyList(), emptyList())
        }

        candidates.forEach { tryTakePersistablePermission(it.uri) }

        val partition = autoMatchUseCase.matchAndPartition(candidates, catalogTracks)
        val matchedEntries = partition.matchedCatalog.mapValues { (trackId, candidate) ->
            LinkedAudioEntry(
                trackId = trackId,
                uriString = candidate.uri.toString(),
                displayName = candidate.displayName,
                durationMs = candidate.metadata.durationMs
            )
        }

        val addedCustomIds = mutableListOf<Int>()

        context.isaiDataStore.edit { prefs ->
            val currentLinked = decodeMap(prefs[Keys.LINKED_AUDIO_JSON]).toMutableMap()
            currentLinked.putAll(matchedEntries)
            prefs[Keys.LINKED_AUDIO_JSON] = encodeMap(currentLinked)

            val existingCustom = decodeCustomTracks(prefs[Keys.CUSTOM_TRACKS_JSON]).toMutableList()
            val linkedUris = currentLinked.values.map { it.uriString }.toSet()
            val customByUri = existingCustom.associateBy { it.uriString }.toMutableMap()
            var nextId = ((existingCustom.maxOfOrNull { it.id } ?: 9_999) + 1).coerceAtLeast(10_000)

            for (candidate in partition.unmatchedFiles) {
                val uriStr = candidate.uri.toString()
                if (linkedUris.contains(uriStr)) continue

                val existing = customByUri[uriStr]
                if (existing != null) {
                    addedCustomIds.add(existing.id)
                    continue
                }

                val title = candidate.metadata.title
                    ?: candidate.displayName.substringBeforeLast('.').ifBlank { candidate.displayName }
                val artist = candidate.metadata.artist ?: "Local Audio"
                val album = candidate.metadata.album ?: "Device Music"

                val custom = CustomDeviceTrack(
                    id = nextId++,
                    uriString = uriStr,
                    displayName = candidate.displayName,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = candidate.metadata.durationMs
                )
                existingCustom.add(custom)
                customByUri[uriStr] = custom
                addedCustomIds.add(custom.id)
            }

            prefs[Keys.CUSTOM_TRACKS_JSON] = json.encodeToString(existingCustom)
            cachedCustomTracks = existingCustom.map { it.toTrack() }.associateBy { it.id }
        }

        return AudioImportSummary(
            totalScanned = candidates.size,
            matchedCatalogCount = matchedEntries.size,
            addedCustomCount = addedCustomIds.size,
            matchedTrackIds = matchedEntries.keys.toList(),
            customTrackIds = addedCustomIds
        )
    }

    suspend fun unlinkTrackAudio(trackId: Int) {
        context.isaiDataStore.edit { prefs ->
            val current = decodeMap(prefs[Keys.LINKED_AUDIO_JSON]).toMutableMap()
            current.remove(trackId)
            prefs[Keys.LINKED_AUDIO_JSON] = encodeMap(current)

            val custom = decodeCustomTracks(prefs[Keys.CUSTOM_TRACKS_JSON])
                .filterNot { it.id == trackId }
            prefs[Keys.CUSTOM_TRACKS_JSON] = json.encodeToString(custom)
            cachedCustomTracks = custom.map { it.toTrack() }.associateBy { it.id }
        }
    }

    suspend fun clearAllLinkedAudio() {
        context.isaiDataStore.edit { prefs ->
            prefs.remove(Keys.LINKED_AUDIO_JSON)
            prefs.remove(Keys.CUSTOM_TRACKS_JSON)
            cachedCustomTracks = emptyMap()
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

    private fun decodeCustomTracks(raw: String?): List<CustomDeviceTrack> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<CustomDeviceTrack>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun CustomDeviceTrack.toTrack(): Track = Track(
        id = id,
        title = title,
        trackType = TrackType.SONG,
        album = album,
        albumKey = "device_local",
        filmOrProject = album,
        language = "Device",
        releaseDate = null,
        composer = artist,
        creditedArtists = emptyList(),
        notes = "Local device audio",
        artworkKey = "artist_mic_studio",
        artworkRes = R.drawable.artist_mic_studio
    )
}
