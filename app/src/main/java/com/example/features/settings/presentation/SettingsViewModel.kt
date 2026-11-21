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

    fun autoMatchMultipleAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val namedFiles = uris.map { uri -> uri to localAudioStore.resolveDisplayName(uri) }
            val matched = autoMatchAudioFilesUseCase(namedFiles, catalogRepository.tracks)
            val count = localAudioStore.linkMultipleTracks(matched)
            if (count > 0) {
                _messages.tryEmit("Auto-matched $count track(s) to local audio!")
            } else {
                _messages.tryEmit("No filename matches found. Link individual tracks by tapping them.")
            }
        }
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
