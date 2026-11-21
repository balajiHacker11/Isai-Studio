package com.example.features.settings.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.AutoMatchAudioFilesUseCase
import com.example.core.local.LocalAudioStore
import com.example.core.model.AccentTheme
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.Track
import com.example.core.model.UserPreferences
import com.example.features.catalog.data.CatalogRepository
import com.example.features.settings.data.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val localAudioStore: LocalAudioStore,
    private val catalogRepository: CatalogRepository,
    private val autoMatchAudioFilesUseCase: AutoMatchAudioFilesUseCase
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.observePreferences()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w("SettingsVM", "Error observing preferences", error)
            emit(UserPreferences())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferences()
        )

    val linkedAudio: StateFlow<Map<Int, LinkedAudioEntry>> = localAudioStore.linkedAudioFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val customTracks: StateFlow<List<Track>> = localAudioStore.customTracksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val updated = transform(preferences.value)
        viewModelScope.launch {
            preferencesRepository.savePreferences(updated).onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Could not save setting")
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) = updatePreferences { it.copy(darkThemeEnabled = enabled) }
    fun setDynamicTint(enabled: Boolean) = updatePreferences { it.copy(dynamicTintEnabled = enabled) }
    fun setAccentTheme(accent: AccentTheme) = updatePreferences { it.copy(accentKey = accent.key) }
    fun setCrossfadeSeconds(seconds: Int) = updatePreferences { it.copy(crossfadeSeconds = seconds.toLong().coerceIn(0L, 12L)) }
    fun setGaplessEnabled(enabled: Boolean) = updatePreferences { it.copy(gaplessEnabled = enabled) }
    fun setNormalizeVolume(enabled: Boolean) = updatePreferences { it.copy(normalizeVolumeEnabled = enabled) }
    fun setPrivateSession(enabled: Boolean) = updatePreferences { it.copy(privateSessionEnabled = enabled) }
    fun setSwipeMiniPlayer(enabled: Boolean) = updatePreferences { it.copy(swipeMiniPlayerEnabled = enabled) }
    fun setSwipeArtwork(enabled: Boolean) = updatePreferences { it.copy(swipeArtworkEnabled = enabled) }
    fun setSwipeRowQueue(enabled: Boolean) = updatePreferences { it.copy(swipeRowQueueEnabled = enabled) }
    fun setSwipeRowLike(enabled: Boolean) = updatePreferences { it.copy(swipeRowLikeEnabled = enabled) }
    fun setDoubleTapSeek(enabled: Boolean) = updatePreferences { it.copy(doubleTapSeekEnabled = enabled) }
    fun setDoubleTapLike(enabled: Boolean) = updatePreferences { it.copy(doubleTapLikeEnabled = enabled) }
    fun setHapticFeedback(enabled: Boolean) = updatePreferences { it.copy(hapticFeedbackEnabled = enabled) }

    fun saveEqualizerSettings(presetName: String, bandsMb: List<Int>) {
        updatePreferences {
            it.copy(
                eqPreset = presetName.take(40).ifBlank { "Studio" },
                eqBands = bandsMb.map { b -> b.toLong() }
            )
        }
    }

    fun linkSingleTrackAudio(track: Track, uri: Uri, onLinked: (() -> Unit)? = null) {
        viewModelScope.launch {
            val entry = localAudioStore.linkTrackAudio(track.id, uri)
            _messages.tryEmit("Linked \"${entry.displayName}\" to ${track.title}")
            onLinked?.invoke()
        }
    }

    fun importMusicFolder(
        treeUri: Uri,
        onComplete: ((matchedIds: List<Int>, customIds: List<Int>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _messages.tryEmit("Scanning music folder…")
            val summary = localAudioStore.importMusicFolder(
                treeUri = treeUri,
                catalogTracks = catalogRepository.tracks,
                autoMatchUseCase = autoMatchAudioFilesUseCase
            )
            emitImportSummary(summary, emptyLabel = "No audio files found in that folder.")
            onComplete?.invoke(summary.matchedTrackIds, summary.customTrackIds)
        }
    }

    fun importMultipleAudioFiles(
        uris: List<Uri>,
        onComplete: ((matchedIds: List<Int>, customIds: List<Int>) -> Unit)? = null
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val summary = localAudioStore.importMultipleAudioUris(
                uris = uris,
                catalogTracks = catalogRepository.tracks,
                autoMatchUseCase = autoMatchAudioFilesUseCase
            )
            emitImportSummary(summary, emptyLabel = "All selected files were already in your library.")
            onComplete?.invoke(summary.matchedTrackIds, summary.customTrackIds)
        }
    }

    private fun emitImportSummary(
        summary: com.example.core.local.AudioImportSummary,
        emptyLabel: String
    ) {
        val msg = when {
            summary.totalScanned == 0 -> emptyLabel
            summary.matchedCatalogCount > 0 && summary.addedCustomCount > 0 ->
                "Added ${summary.totalImported} songs (${summary.matchedCatalogCount} matched, ${summary.addedCustomCount} added to Device Music)"
            summary.matchedCatalogCount > 0 ->
                "Matched ${summary.matchedCatalogCount} catalog song(s)!"
            summary.addedCustomCount > 0 ->
                "Added ${summary.addedCustomCount} song(s) to Device Music!"
            else -> emptyLabel
        }
        _messages.tryEmit(msg)
    }

    fun unlinkTrackAudio(trackId: Int) {
        viewModelScope.launch {
            localAudioStore.unlinkTrackAudio(trackId)
            _messages.tryEmit("Unlinked local audio")
        }
    }

    fun clearAllLinkedAudio() {
        viewModelScope.launch {
            localAudioStore.clearAllLinkedAudio()
            _messages.tryEmit("Cleared all local audio links")
        }
    }
}
