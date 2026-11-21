package com.example.features.library.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.core.local.studioLibraryDataStore
import com.example.core.model.LikedTrack
import com.example.core.model.PlayHistoryEntry
import com.example.core.model.Track
import com.example.core.model.UserPlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class LibraryRepository(context: Context) {
    private val dataStore = context.applicationContext.studioLibraryDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val LIKED_TRACKS = stringPreferencesKey("liked_tracks_json")
        val PLAYLISTS = stringPreferencesKey("playlists_json")
        val PLAY_HISTORY = stringPreferencesKey("play_history_json")
    }

    fun observeLikedTracks(): Flow<List<LikedTrack>> =
        dataStore.data.map { prefs ->
            decodeList<LikedTrack>(prefs[Keys.LIKED_TRACKS])
        }

    fun observePlaylists(): Flow<List<UserPlaylist>> =
        dataStore.data.map { prefs ->
            decodeList<UserPlaylist>(prefs[Keys.PLAYLISTS])
        }

    fun observePlayHistory(): Flow<List<PlayHistoryEntry>> =
        dataStore.data.map { prefs ->
            decodeList<PlayHistoryEntry>(prefs[Keys.PLAY_HISTORY])
                .sortedByDescending { it.playedAtMs }
                .take(40)
        }

    suspend fun toggleLike(track: Track, currentlyLiked: Boolean): Result<Boolean> {
        return try {
            val docId = "track_${track.id}"
            dataStore.edit { prefs ->
                val current = decodeList<LikedTrack>(prefs[Keys.LIKED_TRACKS]).toMutableList()
                if (currentlyLiked) {
                    current.removeAll { it.id == docId || it.trackNumber == track.id.toLong() }
                } else {
                    current.removeAll { it.id == docId || it.trackNumber == track.id.toLong() }
                    current.add(
                        LikedTrack(
                            id = docId,
                            trackNumber = track.id.toLong(),
                            title = track.title.take(160).ifBlank { "Track" },
                            album = track.album.take(160).ifBlank { "Album" },
                            createdAtMs = System.currentTimeMillis()
                        )
                    )
                }
                prefs[Keys.LIKED_TRACKS] = json.encodeToString(current)
            }
            Result.success(!currentlyLiked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(
        name: String,
        description: String,
        coverKey: String = "artist_mic_studio",
        initialTrackIds: List<Int> = emptyList()
    ): Result<String> {
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        val playlistId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()
        return try {
            val playlist = UserPlaylist(
                id = playlistId,
                name = cleanName,
                description = description.trim().take(240),
                trackIds = initialTrackIds.distinct().take(200).map { it.toLong() },
                coverKey = coverKey.take(40).ifBlank { "artist_mic_studio" },
                createdAtMs = now,
                updatedAtMs = now
            )
            dataStore.edit { prefs ->
                val current = decodeList<UserPlaylist>(prefs[Keys.PLAYLISTS]).toMutableList()
                current.add(playlist)
                prefs[Keys.PLAYLISTS] = json.encodeToString(current)
            }
            Result.success(playlistId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistTracks(playlistId: String, trackIds: List<Long>): Result<Unit> {
        return try {
            require(playlistId.isNotBlank())
            val now = System.currentTimeMillis()
            dataStore.edit { prefs ->
                val current = decodeList<UserPlaylist>(prefs[Keys.PLAYLISTS]).map { playlist ->
                    if (playlist.id == playlistId) {
                        playlist.copy(
                            trackIds = trackIds.distinct().take(200),
                            updatedAtMs = now
                        )
                    } else {
                        playlist
                    }
                }
                prefs[Keys.PLAYLISTS] = json.encodeToString(current)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String, newDescription: String): Result<Unit> {
        val cleanName = newName.trim().take(80)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        return try {
            val now = System.currentTimeMillis()
            dataStore.edit { prefs ->
                val current = decodeList<UserPlaylist>(prefs[Keys.PLAYLISTS]).map { playlist ->
                    if (playlist.id == playlistId) {
                        playlist.copy(
                            name = cleanName,
                            description = newDescription.trim().take(240),
                            updatedAtMs = now
                        )
                    } else {
                        playlist
                    }
                }
                prefs[Keys.PLAYLISTS] = json.encodeToString(current)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                val current = decodeList<UserPlaylist>(prefs[Keys.PLAYLISTS])
                    .filterNot { it.id == playlistId }
                prefs[Keys.PLAYLISTS] = json.encodeToString(current)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPlayHistory(track: Track): Result<Unit> {
        val now = System.currentTimeMillis()
        val entry = PlayHistoryEntry(
            id = "hist_${now}_${track.id}",
            trackNumber = track.id.toLong(),
            title = track.title.take(160).ifBlank { "Track" },
            album = track.album.take(160).ifBlank { "Album" },
            playedAtMs = now
        )
        return try {
            dataStore.edit { prefs ->
                val current = decodeList<PlayHistoryEntry>(prefs[Keys.PLAY_HISTORY]).toMutableList()
                current.add(0, entry)
                prefs[Keys.PLAY_HISTORY] = json.encodeToString(current.take(40))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private inline fun <reified T> decodeList(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
    }
}
