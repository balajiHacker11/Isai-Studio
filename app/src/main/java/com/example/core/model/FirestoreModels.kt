package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LikedTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackNumber: Long = 0L,
    val title: String = "",
    val album: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPlaylist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val trackIds: List<Long> = emptyList(),
    val coverKey: String = "artist_mic_studio",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackNumber: Long = 0L,
    val title: String = "",
    val album: String = "",
    @ServerTimestamp val playedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TrackLyrics(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackNumber: Long = 0L,
    val lrcContent: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPreferences(
    @DocumentId val id: String = "",
    val userId: String = "",
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
    val hapticFeedbackEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "darkThemeEnabled" to darkThemeEnabled,
        "dynamicTintEnabled" to dynamicTintEnabled,
        "accentKey" to AccentTheme.fromKey(accentKey).key,
        "crossfadeSeconds" to crossfadeSeconds.coerceIn(0L, 12L),
        "gaplessEnabled" to gaplessEnabled,
        "normalizeVolumeEnabled" to normalizeVolumeEnabled,
        "eqPreset" to eqPreset.take(40).ifBlank { "Studio" },
        "eqBands" to eqBands.take(10),
        "privateSessionEnabled" to privateSessionEnabled,
        "swipeMiniPlayerEnabled" to swipeMiniPlayerEnabled,
        "swipeArtworkEnabled" to swipeArtworkEnabled,
        "swipeRowQueueEnabled" to swipeRowQueueEnabled,
        "swipeRowLikeEnabled" to swipeRowLikeEnabled,
        "doubleTapSeekEnabled" to doubleTapSeekEnabled,
        "doubleTapLikeEnabled" to doubleTapLikeEnabled,
        "hapticFeedbackEnabled" to hapticFeedbackEnabled,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

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
