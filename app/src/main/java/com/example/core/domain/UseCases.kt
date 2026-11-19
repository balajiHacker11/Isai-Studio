package com.example.core.domain

import android.net.Uri
import com.example.core.model.LinkedAudioEntry
import com.example.core.model.LrcLine
import com.example.core.model.Track
import com.example.core.model.TrackType

class SearchCatalogUseCase {
    operator fun invoke(
        tracks: List<Track>,
        query: String,
        trackTypeFilter: TrackType? = null,
        languageFilter: String? = null,
        linkedOnly: Boolean = false,
        linkedAudio: Map<Int, LinkedAudioEntry> = emptyMap()
    ): List<Track> {
        val trimmed = query.trim().lowercase()
        return tracks.filter { track ->
            val matchesType = trackTypeFilter == null || track.trackType == trackTypeFilter
            val matchesLang = languageFilter == null || track.language.equals(languageFilter, ignoreCase = true)
            val matchesLinked = !linkedOnly || linkedAudio.containsKey(track.id)
            val matchesQuery = trimmed.isEmpty() ||
                track.title.lowercase().contains(trimmed) ||
                track.album.lowercase().contains(trimmed) ||
                track.filmOrProject.lowercase().contains(trimmed) ||
                track.language.lowercase().contains(trimmed) ||
                track.creditedArtists.any { it.lowercase().contains(trimmed) }
            matchesType && matchesLang && matchesLinked && matchesQuery
        }
    }
}

class AutoMatchAudioFilesUseCase {
    operator fun invoke(
        files: List<Pair<Uri, String>>,
        catalogTracks: List<Track>
    ): Map<Int, Uri> {
        val matches = mutableMapOf<Int, Uri>()
        val normalizedTracks = catalogTracks.map { track ->
            track to normalize(track.title)
        }

        for ((uri, fileName) in files) {
            val cleanName = normalize(fileName.substringBeforeLast('.'))
            if (cleanName.isBlank()) continue

            val exactMatch = normalizedTracks.firstOrNull { (track, normTitle) ->
                normTitle == cleanName && !matches.containsKey(track.id)
            }
            if (exactMatch != null) {
                matches[exactMatch.first.id] = uri
                continue
            }

            val bestPartial = normalizedTracks
                .filter { (track, normTitle) ->
                    !matches.containsKey(track.id) &&
                        normTitle.length >= 3 &&
                        (cleanName.contains(normTitle) || normTitle.contains(cleanName))
                }
                .maxByOrNull { (_, normTitle) -> normTitle.length }

            if (bestPartial != null) {
                matches[bestPartial.first.id] = uri
            }
        }
        return matches
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

class ParseLrcUseCase {
    private val lineRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")

    operator fun invoke(lrcContent: String): List<LrcLine> {
        if (lrcContent.isBlank()) return emptyList()
        val parsed = mutableListOf<LrcLine>()
        lrcContent.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            val match = lineRegex.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = match.groupValues[3]
                val millis = when (fracRaw.length) {
                    1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fracRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    parsed.add(LrcLine(timestampMs = (min * 60_000L) + (sec * 1_000L) + millis, text = text))
                }
            }
        }
        return parsed.sortedBy { it.timestampMs }
    }

    fun activeLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= positionMs) {
                active = i
            } else {
                break
            }
        }
        return active
    }
}
