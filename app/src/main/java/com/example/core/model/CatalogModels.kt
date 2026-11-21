package com.example.core.model

import androidx.annotation.DrawableRes
import com.example.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogPayload(
    val artist: String = "",
    val scope: String = "",
    @SerialName("generated_on") val generatedOn: String = "",
    val sources: List<String> = emptyList(),
    @SerialName("total_tracks") val totalTracks: Int = 0,
    @SerialName("known_gaps") val knownGaps: List<String> = emptyList(),
    val albums: List<AlbumDto> = emptyList(),
    val tracks: List<TrackDto> = emptyList()
)

@Serializable
data class AlbumDto(
    @SerialName("album_key") val albumKey: String = "",
    val album: String = "",
    @SerialName("film_or_project") val filmOrProject: String = "",
    @SerialName("album_type") val albumType: String = "",
    val language: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    val source: String = "",
    @SerialName("expected_tracks") val expectedTracks: Int? = null,
    @SerialName("tracks_listed") val tracksListed: Int = 0
)

@Serializable
data class TrackDto(
    val id: Int = 0,
    val title: String = "",
    @SerialName("track_type") val trackType: String = "song",
    val album: String = "",
    @SerialName("film_or_project") val filmOrProject: String = "",
    val language: String = "",
    @SerialName("album_release_date") val albumReleaseDate: String? = null,
    val composer: String = "Isai Collective",
    @SerialName("credited_artists") val creditedArtists: List<String> = emptyList(),
    val notes: String? = null,
    @SerialName("asset_path") val assetPath: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("default_lrc") val defaultLrc: String? = null
)

enum class TrackType(val key: String, val label: String) {
    SONG("song", "Song"),
    VERSION("version", "Version"),
    THEME("theme", "Theme"),
    BGM("bgm", "BGM");

    companion object {
        fun fromKey(raw: String): TrackType =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: SONG
    }
}

data class Album(
    val albumKey: String,
    val title: String,
    val filmOrProject: String,
    val albumType: String,
    val language: String,
    val releaseDate: String?,
    val tracksListed: Int,
    val artworkKey: String,
    @DrawableRes val artworkRes: Int
)

data class Track(
    val id: Int,
    val title: String,
    val trackType: TrackType,
    val album: String,
    val albumKey: String,
    val filmOrProject: String,
    val language: String,
    val releaseDate: String?,
    val composer: String,
    val creditedArtists: List<String>,
    val notes: String?,
    val artworkKey: String,
    @DrawableRes val artworkRes: Int,
    val assetPath: String? = null,
    val durationMs: Long = 0L,
    val defaultLrc: String? = null
) {
    val subtitle: String
        get() = buildString {
            append(composer)
            if (creditedArtists.isNotEmpty()) {
                append(" • ")
                append(creditedArtists.joinToString(", "))
            }
        }
}

data class ArtistStorySlide(
    val index: Int,
    val chapter: String,
    val title: String,
    val caption: String,
    val highlightTrackTitle: String,
    val artworkKey: String,
    @DrawableRes val imageRes: Int
)

object ArtistArtwork {
    val portraits: List<Pair<String, Int>> = listOf(
        "artist_mic_studio" to R.drawable.artist_mic_studio,
        "artist_synth_console" to R.drawable.artist_synth_console,
        "artist_daw_session" to R.drawable.artist_daw_session,
        "artist_tekki_smile" to R.drawable.artist_tekki_smile,
        "artist_chill_lounge" to R.drawable.artist_chill_lounge,
        "artist_stage_blazer" to R.drawable.artist_stage_blazer
    )

    val introStories: List<ArtistStorySlide> = listOf(
        ArtistStorySlide(
            index = 0,
            chapter = "01 • INDIE STUDIO SESSIONS",
            title = "Bundled Original MP3 Audio",
            caption = "Step inside Isai Studio — 24 original royalty-free MP3 tracks bundled directly inside the app for instant offline playback.",
            highlightTrackTitle = "Kaveri Dawn • Monsoon Lantern",
            artworkKey = "artist_mic_studio",
            imageRes = R.drawable.artist_mic_studio
        ),
        ArtistStorySlide(
            index = 1,
            chapter = "02 • ANALOG CONSOLE CRAFT",
            title = "Hardware Synths & Brass",
            caption = "Warm detuned oscillators, punchy basslines, and stereo arpeggios mastered into crisp 44.1 kHz MP3 files.",
            highlightTrackTitle = "Oscillator Hearts • Filter Sweep",
            artworkKey = "artist_synth_console",
            imageRes = R.drawable.artist_synth_console
        ),
        ArtistStorySlide(
            index = 2,
            chapter = "03 • MIDNIGHT DAW SCORES",
            title = "Cinematic BGMs & Themes",
            caption = "Explore atmospheric suspense motifs, Lydian orchestral themes, and high-energy chase cues in Nightfall Score.",
            highlightTrackTitle = "Zero Hour Motif • Dawn Over Harbour",
            artworkKey = "artist_daw_session",
            imageRes = R.drawable.artist_daw_session
        ),
        ArtistStorySlide(
            index = 3,
            chapter = "04 • INSTANT MEDIA3 ENGINE",
            title = "Zero-Wait Offline Playback",
            caption = "Tap any song to play immediately, or import your own local MP3/M4A folders anytime using Storage Access Framework.",
            highlightTrackTitle = "Marina Skyline • Sunroof Groove",
            artworkKey = "artist_tekki_smile",
            imageRes = R.drawable.artist_tekki_smile
        ),
        ArtistStorySlide(
            index = 4,
            chapter = "05 • UNPLUGGED & LYRICS",
            title = "Dynamic Art & 5-Band EQ",
            caption = "Color-reactive full player, swipe gestures, hardware Equalizer presets, sleep timer, and synced LRC lyrics.",
            highlightTrackTitle = "Jasmine Breeze • Porchlight Acoustic",
            artworkKey = "artist_chill_lounge",
            imageRes = R.drawable.artist_chill_lounge
        ),
        ArtistStorySlide(
            index = 5,
            chapter = "06 • YOUR LOCAL STUDIO",
            title = "Private Device Library",
            caption = "Curate playlists, favorite tracks, and customize your studio console — saved privately on your device.",
            highlightTrackTitle = "Golden Encore • Final Curtain Theme",
            artworkKey = "artist_stage_blazer",
            imageRes = R.drawable.artist_stage_blazer
        )
    )

    fun drawableForKey(key: String): Int =
        portraits.firstOrNull { it.first == key }?.second ?: R.drawable.artist_mic_studio

    fun artworkForAlbumKey(albumKey: String): Pair<String, Int> = when (albumKey) {
        "indie_voices" -> "artist_mic_studio" to R.drawable.artist_mic_studio
        "analog_console" -> "artist_synth_console" to R.drawable.artist_synth_console
        "nightfall_score" -> "artist_daw_session" to R.drawable.artist_daw_session
        "neon_rhythm" -> "artist_tekki_smile" to R.drawable.artist_tekki_smile
        "unplugged_moods" -> "artist_chill_lounge" to R.drawable.artist_chill_lounge
        "arena_anthems" -> "artist_stage_blazer" to R.drawable.artist_stage_blazer
        else -> "artist_mic_studio" to R.drawable.artist_mic_studio
    }
}
