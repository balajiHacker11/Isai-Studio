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
    val composer: String = "Sai Abhyankkar",
    @SerialName("credited_artists") val creditedArtists: List<String> = emptyList(),
    val notes: String? = null
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
    @DrawableRes val artworkRes: Int
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
            chapter = "01 • THINK INDIE ORIGINS",
            title = "The Voice Behind Katchi Sera",
            caption = "From viral indie melodies to 110+ compositions, step inside Sai Abhyankkar's sonic studio.",
            highlightTrackTitle = "Katchi Sera • Aasa Kooda",
            artworkKey = "artist_mic_studio",
            imageRes = R.drawable.artist_mic_studio
        ),
        ArtistStorySlide(
            index = 1,
            chapter = "02 • ANALOG CONSOLE CRAFT",
            title = "Hardware Synths & Raw Textures",
            caption = "Layering warm analog brass and driving percussion across Tamil, Telugu, and Malayalam scores.",
            highlightTrackTitle = "Sithira Puthiri • Vizhi Veekura",
            artworkKey = "artist_synth_console",
            imageRes = R.drawable.artist_synth_console
        ),
        ArtistStorySlide(
            index = 2,
            chapter = "03 • LATE NIGHT DAW",
            title = "42 Original BGMs & Themes",
            caption = "Explore every score cue from Karuppu Side A & B, Dude, Balti, and Baththa in one place.",
            highlightTrackTitle = "God Mode • Karuppu Title Theme",
            artworkKey = "artist_daw_session",
            imageRes = R.drawable.artist_daw_session
        ),
        ArtistStorySlide(
            index = 3,
            chapter = "04 • YOUTH & ENERGY",
            title = "Built for Seamless Playback",
            caption = "Attach your local MP3 or AAC files with zero-permission matching and stream via Media3 ExoPlayer.",
            highlightTrackTitle = "Oorum Blood • Singari",
            artworkKey = "artist_tekki_smile",
            imageRes = R.drawable.artist_tekki_smile
        ),
        ArtistStorySlide(
            index = 4,
            chapter = "05 • UNPLUGGED MOODS",
            title = "Dynamic Art & Synced Lyrics",
            caption = "Color-reactive mini-player, swipe gestures, 5-band Equalizer, and timestamped LRC lyrics.",
            highlightTrackTitle = "Pavazha Malli Unplugged • Kannukulla",
            artworkKey = "artist_chill_lounge",
            imageRes = R.drawable.artist_chill_lounge
        ),
        ArtistStorySlide(
            index = 5,
            chapter = "06 • YOUR LOCAL STUDIO",
            title = "Curate Your Local Studio",
            caption = "Curate playlists, favorite tracks, and customize your studio settings — saved privately on your device.",
            highlightTrackTitle = "FA FA FA • Mozhiye",
            artworkKey = "artist_stage_blazer",
            imageRes = R.drawable.artist_stage_blazer
        )
    )

    fun drawableForKey(key: String): Int =
        portraits.firstOrNull { it.first == key }?.second ?: R.drawable.artist_mic_studio

    fun artworkForAlbumKey(albumKey: String): Pair<String, Int> = when (albumKey) {
        "indie", "indie_x" -> "artist_mic_studio" to R.drawable.artist_mic_studio
        "benz" -> "artist_synth_console" to R.drawable.artist_synth_console
        "balti" -> "artist_chill_lounge" to R.drawable.artist_chill_lounge
        "dude", "dude_te" -> "artist_tekki_smile" to R.drawable.artist_tekki_smile
        "k_ost", "v" -> "artist_stage_blazer" to R.drawable.artist_stage_blazer
        "k_a", "k_b" -> "artist_daw_session" to R.drawable.artist_daw_session
        "b" -> "artist_synth_console" to R.drawable.artist_synth_console
        "om" -> "artist_mic_studio" to R.drawable.artist_mic_studio
        else -> "artist_mic_studio" to R.drawable.artist_mic_studio
    }
}
