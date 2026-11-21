package com.example.features.player.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.ParseLrcUseCase
import com.example.core.local.LocalAudioStore
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.LrcLine
import com.example.core.model.Track
import com.example.core.player.AudioFxBridge
import com.example.core.player.EqualizerState
import com.example.core.player.PlaybackCoordinator
import com.example.core.player.PlaybackSnapshot
import com.example.features.library.data.LibraryRepository
import com.example.features.player.data.LyricsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqPreset(val name: String, val bandsMb: List<Int>)

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val playbackCoordinator: PlaybackCoordinator,
    private val localAudioStore: LocalAudioStore,
    private val libraryRepository: LibraryRepository,
    private val lyricsRepository: LyricsRepository,
    private val parseLrcUseCase: ParseLrcUseCase
) : ViewModel() {

    val eqPresets: List<EqPreset> = listOf(
        EqPreset("Flat", listOf(0, 0, 0, 0, 0)),
        EqPreset("Studio", listOf(150, 50, 0, 100, 200)),
        EqPreset("Bass Boost", listOf(550, 350, 100, 0, -50)),
        EqPreset("Mass Theatre", listOf(450, 200, -100, 250, 400)),
        EqPreset("Acoustic", listOf(100, 200, 250, 150, 100)),
        EqPreset("Vocal", listOf(-150, 100, 400, 350, 100))
    )

    val snapshot: StateFlow<PlaybackSnapshot> = playbackCoordinator.snapshot
    val eqState: StateFlow<EqualizerState> = AudioFxBridge.eqState

    private val _activeEqPreset = MutableStateFlow("Studio")
    val activeEqPreset: StateFlow<String> = _activeEqPreset.asStateFlow()

    private val _isFullPlayerExpanded = MutableStateFlow(false)
    val isFullPlayerExpanded: StateFlow<Boolean> = _isFullPlayerExpanded.asStateFlow()

    private val _unlinkedTrackPrompt = MutableStateFlow<Track?>(null)
    val unlinkedTrackPrompt: StateFlow<Track?> = _unlinkedTrackPrompt.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var privateSessionEnabled: Boolean = false

    val linkedAudio: StateFlow<Map<Int, LinkedAudioEntry>> = localAudioStore.linkedAudioFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap()
        )

    val activeTrackRawLrc: StateFlow<String> = snapshot
        .map { it.currentTrack }
        .distinctUntilChanged { old, new -> old?.id == new?.id }
        .flatMapLatest { track ->
            if (track == null) {
                flowOf("")
            } else {
                lyricsRepository.observeTrackLyrics(track.id)
                    .map { saved -> saved?.lrcContent ?: track.defaultLrc.orEmpty() }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w("PlayerVM", "Error observing track lyrics", error)
                        emit(track.defaultLrc.orEmpty())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ""
        )

    val parsedLyrics: StateFlow<List<LrcLine>> = activeTrackRawLrc
        .map { raw -> parseLrcUseCase(raw) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        playbackCoordinator.connect()

        viewModelScope.launch {
            playbackCoordinator.trackStartedEvents.collect { track ->
                if (!privateSessionEnabled) {
                    libraryRepository.recordPlayHistory(track)
                }
            }
        }

        viewModelScope.launch {
            playbackCoordinator.unlinkedTrackRequests.collect { track ->
                _unlinkedTrackPrompt.value = track
            }
        }
    }

    fun setPrivateSession(enabled: Boolean) {
        privateSessionEnabled = enabled
    }

    fun applyAudioSettings(crossfadeSec: Int, normalizeVolume: Boolean, preset: String, eqBands: List<Long>) {
        playbackCoordinator.updateAudioSettings(crossfadeSec, normalizeVolume)
        _activeEqPreset.value = preset
        if (eqBands.isNotEmpty()) {
            AudioFxBridge.applyAllBands(eqBands.map { it.toInt() })
        }
    }

    fun expandPlayer() {
        _isFullPlayerExpanded.value = true
    }

    fun minimizePlayer() {
        _isFullPlayerExpanded.value = false
    }

    fun dismissUnlinkedPrompt() {
        _unlinkedTrackPrompt.value = null
    }

    fun playTrack(track: Track, contextTracks: List<Track>) {
        playbackCoordinator.playTrack(track, contextTracks, linkedAudio.value)
    }

    fun playAfterImport(
        preferredTrack: Track?,
        matchedIds: List<Int>,
        customIds: List<Int>,
        catalogTracks: List<Track>,
        fallbackContext: List<Track>?
    ) {
        viewModelScope.launch {
            val latestLinked = localAudioStore.linkedAudioFlow.first()
            val customTracks = localAudioStore.customTracksFlow.first()
            val allMerged = catalogTracks + customTracks

            val targetTrack = when {
                preferredTrack != null && latestLinked.containsKey(preferredTrack.id) -> preferredTrack
                matchedIds.isNotEmpty() -> allMerged.firstOrNull { it.id == matchedIds.first() }
                customIds.isNotEmpty() -> allMerged.firstOrNull { it.id == customIds.first() }
                else -> null
            } ?: return@launch

            val queue = buildList {
                val baseList = fallbackContext?.takeIf { it.isNotEmpty() } ?: allMerged
                add(targetTrack)
                baseList.forEach { candidate ->
                    if (candidate.id != targetTrack.id && latestLinked.containsKey(candidate.id)) {
                        add(candidate)
                    }
                }
                allMerged.forEach { candidate ->
                    if (candidate.id != targetTrack.id && latestLinked.containsKey(candidate.id) && none { it.id == candidate.id }) {
                        add(candidate)
                    }
                }
            }
            playbackCoordinator.playTrack(targetTrack, queue, latestLinked)
        }
    }

    fun playNext(track: Track) {
        if (playbackCoordinator.playNext(track, linkedAudio.value)) {
            _snackbarMessages.tryEmit("\"${track.title}\" will play next")
        }
    }

    fun addToQueue(track: Track) {
        if (playbackCoordinator.addToQueue(track, linkedAudio.value)) {
            _snackbarMessages.tryEmit("Added \"${track.title}\" to queue")
        }
    }

    fun togglePlayPause() = playbackCoordinator.togglePlayPause()
    fun skipNext() = playbackCoordinator.skipNext()
    fun skipPrevious() = playbackCoordinator.skipPrevious()
    fun seekTo(positionMs: Long) = playbackCoordinator.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = playbackCoordinator.seekBy(deltaMs)
    fun toggleShuffle() = playbackCoordinator.toggleShuffle()
    fun cycleRepeatMode() = playbackCoordinator.cycleRepeatMode()
    fun jumpToQueueIndex(index: Int) = playbackCoordinator.jumpToQueueIndex(index)
    fun moveQueueItem(from: Int, to: Int) = playbackCoordinator.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = playbackCoordinator.removeQueueItem(index)

    fun startSleepTimer(minutes: Int) {
        playbackCoordinator.startSleepTimer(minutes)
        _snackbarMessages.tryEmit("Sleep timer set for $minutes min")
    }

    fun enableSleepAtEndOfTrack() {
        playbackCoordinator.enableSleepAtEndOfTrack()
        _snackbarMessages.tryEmit("Playback will pause at end of track")
    }

    fun cancelSleepTimer() {
        playbackCoordinator.cancelSleepTimer()
        _snackbarMessages.tryEmit("Sleep timer cancelled")
    }

    fun selectEqPreset(preset: EqPreset) {
        _activeEqPreset.value = preset.name
        AudioFxBridge.applyAllBands(preset.bandsMb)
    }

    fun setEqBand(bandIndex: Int, levelMb: Int) {
        _activeEqPreset.value = "Custom"
        AudioFxBridge.setBandLevel(bandIndex, levelMb)
    }

    fun saveLyrics(trackId: Int, lrcContent: String) {
        viewModelScope.launch {
            lyricsRepository.saveTrackLyrics(trackId, lrcContent).fold(
                onSuccess = { _snackbarMessages.tryEmit("Synced LRC lyrics saved") },
                onFailure = { err -> _snackbarMessages.tryEmit(err.localizedMessage ?: "Failed to save lyrics") }
            )
        }
    }

    fun activeLyricIndex(lines: List<LrcLine>, positionMs: Long): Int =
        parseLrcUseCase.activeLineIndex(lines, positionMs)

    fun clearPlaybackError() = playbackCoordinator.clearPlaybackError()
}
