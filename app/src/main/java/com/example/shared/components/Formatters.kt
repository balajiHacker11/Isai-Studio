package com.example.shared.components

import android.content.Context
import android.content.Intent
import com.example.core.model.Track

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}

fun shareTrackIntent(context: Context, track: Track) {
    val shareText = buildString {
        append("Listening to \"${track.title}\" by ${track.composer}")
        if (track.creditedArtists.isNotEmpty()) {
            append(" ft. ${track.creditedArtists.joinToString(", ")}")
        }
        append("\nAlbum: ${track.album} (${track.language})")
        append("\nPlayed on Isai Studio")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.composer}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Track"))
}
