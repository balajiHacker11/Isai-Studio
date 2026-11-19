package com.example.features.catalog.data

import android.content.Context
import com.example.core.model.Album
import com.example.core.model.ArtistArtwork
import com.example.core.model.CatalogPayload
import com.example.core.model.Track
import com.example.core.model.TrackType
import kotlinx.serialization.json.Json

class CatalogRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val loadedCatalog: Pair<List<Album>, List<Track>> by lazy {
        loadFromAssets()
    }

    val albums: List<Album> get() = loadedCatalog.first
    val tracks: List<Track> get() = loadedCatalog.second

    fun trackById(id: Int): Track? = tracks.firstOrNull { it.id == id }

    fun albumByKey(albumKey: String): Album? = albums.firstOrNull { it.albumKey == albumKey }

    fun tracksForAlbum(albumKey: String): List<Track> = tracks.filter { it.albumKey == albumKey }

    private fun loadFromAssets(): Pair<List<Album>, List<Track>> {
        val rawJson = context.assets.open("sai_abhyankkar_catalog.json")
            .bufferedReader()
            .use { it.readText() }
        val payload = json.decodeFromString<CatalogPayload>(rawJson)

        val albums = payload.albums.map { dto ->
            val (artKey, artRes) = ArtistArtwork.artworkForAlbumKey(dto.albumKey)
            Album(
                albumKey = dto.albumKey,
                title = dto.album,
                filmOrProject = dto.filmOrProject,
                albumType = dto.albumType,
                language = dto.language,
                releaseDate = dto.releaseDate,
                tracksListed = dto.tracksListed,
                artworkKey = artKey,
                artworkRes = artRes
            )
        }

        val albumKeyByTitle = albums.associateBy({ it.title }, { it.albumKey })

        val tracks = payload.tracks.map { dto ->
            val albumKey = albumKeyByTitle[dto.album] ?: "indie"
            val (artKey, artRes) = ArtistArtwork.artworkForAlbumKey(albumKey)
            Track(
                id = dto.id,
                title = dto.title,
                trackType = TrackType.fromKey(dto.trackType),
                album = dto.album,
                albumKey = albumKey,
                filmOrProject = dto.filmOrProject,
                language = dto.language,
                releaseDate = dto.albumReleaseDate,
                composer = dto.composer,
                creditedArtists = dto.creditedArtists,
                notes = dto.notes,
                artworkKey = artKey,
                artworkRes = artRes
            )
        }

        return albums to tracks
    }
}
