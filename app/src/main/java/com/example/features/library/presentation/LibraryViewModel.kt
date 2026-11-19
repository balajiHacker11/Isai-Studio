package com.example.features.library.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.LikedTrack
import com.example.core.model.PlayHistoryEntry
import com.example.core.model.Track
import com.example.core.model.UiState
import com.example.core.model.UserPlaylist
import com.example.features.library.data.LibraryRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val currentUserId: String
) : ViewModel() {

    val likedTracksState: StateFlow<UiState<List<LikedTrack>>> =
        repository.observeLikedTracks(currentUserId)
            .map<List<LikedTrack>, UiState<List<LikedTrack>>> { list ->
                UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("LibraryVM", "Error observing liked tracks", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load liked tracks"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val likedTrackIds: StateFlow<Set<Int>> = likedTracksState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.map { it.trackNumber.toInt() }.toSet()
                else -> emptySet()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptySet()
        )

    val playlistsState: StateFlow<UiState<List<UserPlaylist>>> =
        repository.observePlaylists(currentUserId)
            .map<List<UserPlaylist>, UiState<List<UserPlaylist>>> { list ->
                UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: 0L })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("LibraryVM", "Error observing playlists", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load playlists"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val historyState: StateFlow<UiState<List<PlayHistoryEntry>>> =
        repository.observePlayHistory(currentUserId)
            .map<List<PlayHistoryEntry>, UiState<List<PlayHistoryEntry>>> { list ->
                UiState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("LibraryVM", "Error observing play history", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(400L)
            _isRefreshing.value = false
        }
    }

    fun toggleLike(track: Track) {
        val currentlyLiked = likedTrackIds.value.contains(track.id)
        viewModelScope.launch {
            repository.toggleLike(currentUserId, track, currentlyLiked).fold(
                onSuccess = { nowLiked ->
                    _snackbarMessages.tryEmit(
                        if (nowLiked) "Added \"${track.title}\" to Liked Songs"
                        else "Removed \"${track.title}\" from Liked Songs"
                    )
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Could not update liked track")
                }
            )
        }
    }

    fun createPlaylist(
        name: String,
        description: String,
        coverKey: String,
        initialTrackId: Int? = null
    ) {
        viewModelScope.launch {
            val initialIds = listOfNotNull(initialTrackId)
            repository.createPlaylist(currentUserId, name, description, coverKey, initialIds).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit("Created playlist \"$name\"")
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Failed to create playlist")
                }
            )
        }
    }

    fun addTrackToPlaylist(playlist: UserPlaylist, track: Track) {
        val trackNum = track.id.toLong()
        if (playlist.trackIds.contains(trackNum)) {
            _snackbarMessages.tryEmit("\"${track.title}\" is already in ${playlist.name}")
            return
        }
        val updated = playlist.trackIds + trackNum
        viewModelScope.launch {
            repository.updatePlaylistTracks(currentUserId, playlist.id, updated).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit("Added \"${track.title}\" to ${playlist.name}")
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Could not add track")
                }
            )
        }
    }

    fun removeTrackFromPlaylist(playlist: UserPlaylist, trackId: Int) {
        val updated = playlist.trackIds.filterNot { it == trackId.toLong() }
        viewModelScope.launch {
            repository.updatePlaylistTracks(currentUserId, playlist.id, updated)
        }
    }

    fun reorderPlaylistTrack(playlist: UserPlaylist, fromIndex: Int, toIndex: Int) {
        val mutable = playlist.trackIds.toMutableList()
        if (fromIndex !in mutable.indices || toIndex !in mutable.indices) return
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        viewModelScope.launch {
            repository.updatePlaylistTracks(currentUserId, playlist.id, mutable)
        }
    }

    fun renamePlaylist(playlist: UserPlaylist, newName: String, newDescription: String) {
        viewModelScope.launch {
            repository.renamePlaylist(currentUserId, playlist.id, newName, newDescription).fold(
                onSuccess = { _snackbarMessages.tryEmit("Updated playlist") },
                onFailure = { err -> _snackbarMessages.tryEmit(err.localizedMessage ?: "Update failed") }
            )
        }
    }

    fun deletePlaylist(playlist: UserPlaylist) {
        viewModelScope.launch {
            repository.deletePlaylist(currentUserId, playlist.id).fold(
                onSuccess = { _snackbarMessages.tryEmit("Deleted \"${playlist.name}\"") },
                onFailure = { err -> _snackbarMessages.tryEmit(err.localizedMessage ?: "Delete failed") }
            )
        }
    }
}
