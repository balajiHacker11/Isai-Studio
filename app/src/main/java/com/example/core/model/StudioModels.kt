package com.example.core.model

import kotlinx.serialization.Serializable

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AccentTheme(val key: String, val label: String) {
    CORAL("coral", "Coral Studio"),
    CYAN("cyan", "Electric Cyan"),
    VIOLET("violet", "Royal Violet"),
    GOLD("gold", "Gold Vinyl");

    companion object {
        fun fromKey(raw: String?): AccentTheme =
            entries.firstOrNull { it.key == raw } ?: CORAL
    }
}

@Serializable
data class LikedTrack(
    val id: String = "",
    val trackNumber: Long = 0L,
    val title: String = "",
    val album: String = "",
    val createdAtMs: Long = 0L
)

@Serializable
data class UserPlaylist(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val trackIds: List<Long> = emptyList(),
    val coverKey: String = "artist_mic_studio",
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

@Serializable
data class PlayHistoryEntry(
    val id: String = "",
    val trackNumber: Long = 0L,
    val title: String = "",
    val album: String = "",
    val playedAtMs: Long = 0L
)

@Serializable
data class TrackLyrics(
    val trackNumber: Long = 0L,
    val lrcContent: String = "",
    val updatedAtMs: Long = 0L
)

@Serializable
data class UserPreferences(
    val darkThemeEnabled: Boolean = true,
    val dynamicTintEnabled: Boolean = true,
    val accentKey: String = AccentTheme.CORAL.key,
    val crossfadeSeconds: Long = 3L,
    val gaplessEnabled: Boolean = true,
    val normalizeVolumeEnabled: Boolean = false,
    val eqPreset: String = "Studio",
    val eqBands: List<Long> = listOf(150L, 50L, 0L, 100L, 200L),
    val privateSessionEnabled: Boolean = false,
    val swipeMiniPlayerEnabled: Boolean = true,
    val swipeArtworkEnabled: Boolean = true,
    val swipeRowQueueEnabled: Boolean = true,
    val swipeRowLikeEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = true,
    val doubleTapLikeEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
)

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

data class LinkedAudioEntry(
    val trackId: Int,
    val uriString: String,
    val displayName: String,
    val durationMs: Long = 0L
)
