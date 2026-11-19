package com.example.core.player

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val available: Boolean = false,
    val bandCount: Int = 5,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val centerFreqsHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val bandLevelsMb: List<Int> = listOf(150, 50, 0, 100, 200)
)

object AudioFxBridge {
    private val _eqState = MutableStateFlow(EqualizerState())
    val eqState: StateFlow<EqualizerState> = _eqState.asStateFlow()

    private var equalizer: Equalizer? = null

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        runCatching {
            equalizer?.release()
            val eq = Equalizer(0, audioSessionId).apply { enabled = true }
            equalizer = eq
            val bands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            val freqs = (0 until bands).map { band -> eq.getCenterFreq(band.toShort()) / 1000 }
            val currentLevels = _eqState.value.bandLevelsMb
            val appliedLevels = (0 until bands).map { idx ->
                val target = currentLevels.getOrElse(idx) { 0 }
                    .coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(idx.toShort(), target.toShort())
                target
            }
            _eqState.value = EqualizerState(
                available = true,
                bandCount = bands,
                minLevelMb = range[0].toInt(),
                maxLevelMb = range[1].toInt(),
                centerFreqsHz = freqs,
                bandLevelsMb = appliedLevels
            )
        }.onFailure { err ->
            Log.w("AudioFxBridge", "Hardware equalizer unavailable: ${err.message}")
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val current = _eqState.value
        val clamped = levelMb.coerceIn(current.minLevelMb, current.maxLevelMb)
        val updated = current.bandLevelsMb.toMutableList()
        while (updated.size <= bandIndex) updated.add(0)
        updated[bandIndex] = clamped
        _eqState.value = current.copy(bandLevelsMb = updated)

        runCatching {
            equalizer?.let { eq ->
                if (bandIndex < eq.numberOfBands) {
                    eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
                }
            }
        }
    }

    fun applyAllBands(levelsMb: List<Int>) {
        val current = _eqState.value
        val updated = (0 until current.bandCount).map { idx ->
            levelsMb.getOrElse(idx) { 0 }.coerceIn(current.minLevelMb, current.maxLevelMb)
        }
        _eqState.value = current.copy(bandLevelsMb = updated)
        runCatching {
            equalizer?.let { eq ->
                updated.forEachIndexed { idx, lvl ->
                    if (idx < eq.numberOfBands) {
                        eq.setBandLevel(idx.toShort(), lvl.toShort())
                    }
                }
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
    }
}

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioFxBridge.attachToAudioSession(audioSessionId)
            }
        })

        AudioFxBridge.attachToAudioSession(exoPlayer.audioSessionId)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioFxBridge.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
