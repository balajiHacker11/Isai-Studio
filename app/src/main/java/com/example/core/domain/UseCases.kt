package com.example.core.domain

import android.net.Uri
import com.example.core.local.AudioFileMetadata
import com.example.core.local.AudioImportCandidate
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

data class AudioMatchPartition(
    val matchedCatalog: Map<Int, AudioImportCandidate>,
    val unmatchedFiles: List<AudioImportCandidate>
)

class AutoMatchAudioFilesUseCase {
    operator fun invoke(
        files: List<Pair<Uri, String>>,
        catalogTracks: List<Track>
    ): Map<Int, Uri> {
        val candidates = files.map { (uri, name) ->
            AudioImportCandidate(
                uri = uri,
                displayName = name,
                metadata = AudioFileMetadata(null, null, null, 0L)
            )
        }
        return matchAndPartition(candidates, catalogTracks).matchedCatalog.mapValues { it.value.uri }
    }

    fun matchAndPartition(
        candidates: List<AudioImportCandidate>,
        catalogTracks: List<Track>
    ): AudioMatchPartition {
        val matched = mutableMapOf<Int, AudioImportCandidate>()
        val unmatched = mutableListOf<AudioImportCandidate>()

        val normalizedTracks = catalogTracks.map { track ->
            val spaced = normalize(track.title)
            val collapsed = spaced.replace(" ", "")
            Triple(track, spaced, collapsed)
        }

        for (candidate in candidates) {
            val fileBase = candidate.displayName.substringBeforeLast('.')
            val rawInputs = listOfNotNull(candidate.metadata.title, fileBase)
                .map { cleanNoise(normalize(it)) }
                .filter { it.isNotBlank() }

            var matchedTrackId: Int? = null

            for (cleanInput in rawInputs) {
                val collapsedInput = cleanInput.replace(" ", "")

                val exact = normalizedTracks.firstOrNull { (track, spaced, collapsed) ->
                    !matched.containsKey(track.id) &&
                        (spaced == cleanInput || (collapsed.length >= 4 && collapsed == collapsedInput))
                }
                if (exact != null) {
                    matchedTrackId = exact.first.id
                    break
                }

                val partial = normalizedTracks
                    .filter { (track, spaced, collapsed) ->
                        !matched.containsKey(track.id) &&
                            collapsed.length >= 4 &&
                            (cleanInput.contains(spaced) ||
                                spaced.contains(cleanInput) ||
                                collapsedInput.contains(collapsed))
                    }
                    .maxByOrNull { (_, spaced, _) -> spaced.length }

                if (partial != null) {
                    matchedTrackId = partial.first.id
                    break
                }
            }

            if (matchedTrackId != null) {
                matched[matchedTrackId] = candidate
            } else {
                unmatched.add(candidate)
            }
        }

        return AudioMatchPartition(matchedCatalog = matched, unmatchedFiles = unmatched)
    }

    private fun cleanNoise(normalized: String): String {
        val noise = setOf("320kbps", "128kbps", "mp3", "aac", "flac", "wav", "m4a", "official", "lyric", "lyrical", "video", "audio", "song", "masstamilan", "ringtone")
        return normalized.split(' ')
            .filter { token -> token.isNotBlank() && !noise.contains(token) }
            .joinToString(" ")
            .trim()
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
