package com.example.core.player

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.example.core.local.LocalAudioStore
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.Track
import com.example.features.catalog.data.CatalogRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaybackSnapshot(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val dominantColor: Color = Color(0xFF282038),
    val vibrantColor: Color = Color(0xFFFF6B4A),
    val sleepTimerRemainingMs: Long? = null,
    val sleepAtEndOfTrack: Boolean = false,
    val playbackError: String? = null
)

@OptIn(UnstableApi::class)
class PlaybackCoordinator(
    private val context: Context,
    private val catalogRepository: CatalogRepository,
    private val localAudioStore: LocalAudioStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val _trackStartedEvents = MutableSharedFlow<Track>(extraBufferCapacity = 4)
    val trackStartedEvents: SharedFlow<Track> = _trackStartedEvents.asSharedFlow()

    private val _unlinkedTrackRequests = MutableSharedFlow<Track>(extraBufferCapacity = 4)
    val unlinkedTrackRequests: SharedFlow<Track> = _unlinkedTrackRequests.asSharedFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var crossfadeSeconds: Int = 3
    private var normalizeVolumeEnabled: Boolean = false
    private var lastRecordedTrackId: Int? = null
    private val paletteCache = mutableMapOf<Int, Pair<Color, Color>>()
    private var pendingPlayRequest: Triple<Track, List<Track>, Map<Int, LinkedAudioEntry>>? = null

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                attachPlayerListener(controller)
                syncFromPlayer(controller)
                startProgressLoop()
                pendingPlayRequest?.let { (track, contextTracks, linked) ->
                    pendingPlayRequest = null
                    playTrack(track, contextTracks, linked)
                }
            } catch (e: Exception) {
                Log.e("PlaybackCoordinator", "Failed to connect MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    fun updateAudioSettings(crossfadeSec: Int, normalizeVolume: Boolean) {
        crossfadeSeconds = crossfadeSec.coerceIn(0, 12)
        normalizeVolumeEnabled = normalizeVolume
        mediaController?.volume = if (normalizeVolumeEnabled) 0.85f else 1.0f
    }

    fun playTrack(
        track: Track,
        contextTracks: List<Track>,
        linkedAudio: Map<Int, LinkedAudioEntry>
    ): Boolean {
        val controller = mediaController ?: run {
            pendingPlayRequest = Triple(track, contextTracks, linkedAudio)
            connect()
            return false
        }

        val playableQueue = buildList {
            add(track)
            contextTracks.forEach { candidate ->
                if (candidate.id != track.id) {
                    add(candidate)
                }
            }
        }

        val mediaItems = playableQueue.map { item ->
            val uriString = resolveTrackAudioUri(item, linkedAudio)
            buildMediaItem(item, uriString)
        }

        if (mediaItems.isEmpty()) return false

        _snapshot.update { it.copy(playbackError = null, queue = playableQueue, currentTrack = track, currentIndex = 0) }
        extractArtworkColors(track.artworkRes)
        controller.setMediaItems(mediaItems, 0, 0L)
        controller.prepare()
        controller.volume = if (normalizeVolumeEnabled) 0.85f else 1.0f
        controller.play()
        emitTrackStartedIfNew(track)
        return true
    }

    fun playNext(track: Track, linkedAudio: Map<Int, LinkedAudioEntry>): Boolean {
        val uriString = resolveTrackAudioUri(track, linkedAudio)
        val controller = mediaController ?: return false
        if (controller.mediaItemCount == 0) {
            return playTrack(track, listOf(track), linkedAudio)
        }
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
        controller.addMediaItem(insertIndex, buildMediaItem(track, uriString))
        syncFromPlayer(controller)
        return true
    }

    fun addToQueue(track: Track, linkedAudio: Map<Int, LinkedAudioEntry>): Boolean {
        val uriString = resolveTrackAudioUri(track, linkedAudio)
        val controller = mediaController ?: return false
        if (controller.mediaItemCount == 0) {
            return playTrack(track, listOf(track), linkedAudio)
        }
        controller.addMediaItem(buildMediaItem(track, uriString))
        syncFromPlayer(controller)
        return true
    }

    private fun resolveTrackAudioUri(track: Track, linkedAudio: Map<Int, LinkedAudioEntry>): String {
        linkedAudio[track.id]?.uriString?.let { return it }
        track.assetPath?.let { path -> return "asset:///$path" }
        return StudioAudioSynth.uriForTrack(track.id)
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
            controller.moveMediaItem(fromIndex, toIndex)
            syncFromPlayer(controller)
        }
    }

    fun removeQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
            syncFromPlayer(controller)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val maxDuration = controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        controller.seekTo(positionMs.coerceIn(0L, maxDuration))
        _snapshot.update { it.copy(positionMs = controller.currentPosition.coerceAtLeast(0L)) }
    }

    fun seekBy(deltaMs: Long) {
        val controller = mediaController ?: return
        seekTo(controller.currentPosition + deltaMs)
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekToDefaultPosition(0)
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        _snapshot.update { it.copy(shuffleEnabled = controller.shuffleModeEnabled) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _snapshot.update { it.copy(repeatMode = nextMode) }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalMs = minutes * 60_000L
        _snapshot.update { it.copy(sleepTimerRemainingMs = totalMs, sleepAtEndOfTrack = false) }
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0L && isActive) {
                delay(1000L)
                remaining -= 1000L
                _snapshot.update { it.copy(sleepTimerRemainingMs = remaining.coerceAtLeast(0L)) }
                if (remaining in 1L..3000L) {
                    val baseVol = if (normalizeVolumeEnabled) 0.85f else 1.0f
                    mediaController?.volume = baseVol * (remaining / 3000f)
                }
            }
            if (isActive) {
                mediaController?.pause()
                mediaController?.volume = if (normalizeVolumeEnabled) 0.85f else 1.0f
                _snapshot.update { it.copy(sleepTimerRemainingMs = null, sleepAtEndOfTrack = false) }
            }
        }
    }

    fun enableSleepAtEndOfTrack() {
        sleepTimerJob?.cancel()
        _snapshot.update { it.copy(sleepTimerRemainingMs = null, sleepAtEndOfTrack = true) }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        mediaController?.volume = if (normalizeVolumeEnabled) 0.85f else 1.0f
        _snapshot.update { it.copy(sleepTimerRemainingMs = null, sleepAtEndOfTrack = false) }
    }

    fun clearPlaybackError() {
        _snapshot.update { it.copy(playbackError = null) }
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _snapshot.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (_snapshot.value.sleepAtEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    controller.pause()
                    _snapshot.update { it.copy(sleepAtEndOfTrack = false) }
                }
                syncFromPlayer(controller)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && _snapshot.value.sleepAtEndOfTrack) {
                    _snapshot.update { it.copy(sleepAtEndOfTrack = false) }
                }
                syncFromPlayer(controller)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlaybackCoordinator", "ExoPlayer error: ${error.errorCodeName}", error)
                _snapshot.update {
                    it.copy(
                        isPlaying = false,
                        playbackError = "Unable to play audio for this track."
                    )
                }
            }
        })
    }

    private fun syncFromPlayer(controller: MediaController) {
        val count = controller.mediaItemCount
        val tracks = (0 until count).mapNotNull { idx ->
            val id = controller.getMediaItemAt(idx).mediaId.toIntOrNull()
            id?.let { catalogRepository.trackById(it) ?: localAudioStore.customTrackById(it) }
        }
        val currentIdx = controller.currentMediaItemIndex
        val currentTrack = tracks.getOrNull(currentIdx)
        currentTrack?.let {
            extractArtworkColors(it.artworkRes)
            emitTrackStartedIfNew(it)
        }
        _snapshot.update {
            it.copy(
                queue = tracks,
                currentIndex = currentIdx,
                currentTrack = currentTrack,
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.takeIf { d -> d > 0 } ?: 0L,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0 } ?: 0L
                    _snapshot.update { it.copy(positionMs = pos, durationMs = dur) }

                    // Smooth crossfade envelope near track boundaries when enabled
                    if (crossfadeSeconds > 0 && dur > crossfadeSeconds * 2000L && controller.isPlaying) {
                        val fadeMs = crossfadeSeconds * 1000L
                        val baseVol = if (normalizeVolumeEnabled) 0.85f else 1.0f
                        val remaining = dur - pos
                        val envelope = when {
                            pos < fadeMs -> (pos.toFloat() / fadeMs).coerceIn(0.2f, 1f)
                            remaining in 1L..fadeMs -> (remaining.toFloat() / fadeMs).coerceIn(0.2f, 1f)
                            else -> 1f
                        }
                        if (_snapshot.value.sleepTimerRemainingMs == null) {
                            controller.volume = baseVol * envelope
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    private fun emitTrackStartedIfNew(track: Track) {
        if (lastRecordedTrackId != track.id) {
            lastRecordedTrackId = track.id
            _trackStartedEvents.tryEmit(track)
        }
    }

    private fun extractArtworkColors(artworkRes: Int) {
        paletteCache[artworkRes]?.let { (dom, vib) ->
            _snapshot.update { it.copy(dominantColor = dom, vibrantColor = vib) }
            return
        }
        scope.launch(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeResource(context.resources, artworkRes) ?: return@launch
            val palette = Palette.from(bitmap).generate()
            val domInt = palette.getDarkMutedColor(0xFF1F2233.toInt())
            val vibInt = palette.getVibrantColor(palette.getLightVibrantColor(0xFFFF6B4A.toInt()))
            val dom = Color(domInt)
            val vib = Color(vibInt)
            paletteCache[artworkRes] = dom to vib
            withContext(Dispatchers.Main) {
                _snapshot.update { it.copy(dominantColor = dom, vibrantColor = vib) }
            }
        }
    }

    private fun buildMediaItem(track: Track, uriString: String): MediaItem {
        val artworkUri = Uri.parse("android.resource://${context.packageName}/${track.artworkRes}")
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.subtitle)
            .setAlbumTitle(track.album)
            .setArtworkUri(artworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(Uri.parse(uriString))
            .setMediaMetadata(metadata)
            .build()
    }
}
