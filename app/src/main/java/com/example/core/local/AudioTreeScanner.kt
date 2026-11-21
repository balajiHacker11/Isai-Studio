package com.example.core.local

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns

data class DiscoveredAudioFile(
    val uri: Uri,
    val displayName: String
)

data class AudioFileMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long
)

data class AudioImportCandidate(
    val uri: Uri,
    val displayName: String,
    val metadata: AudioFileMetadata
)

data class AudioImportSummary(
    val totalScanned: Int,
    val matchedCatalogCount: Int,
    val addedCustomCount: Int,
    val matchedTrackIds: List<Int>,
    val customTrackIds: List<Int>
) {
    val totalImported: Int get() = matchedCatalogCount + addedCustomCount
}

object AudioTreeScanner {
    private val audioExtensions = setOf(
        "mp3", "aac", "m4a", "flac", "wav", "ogg", "opus", "wma", "amr", "3gp", "webm", "mid", "midi"
    )

    fun scanFolder(context: Context, treeUri: Uri): List<DiscoveredAudioFile> {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Best-effort persistable permission for SAF tree.
        }

        val rootDocId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return emptyList()

        val results = mutableListOf<DiscoveredAudioFile>()
        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        queue.add(rootDocId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        while (queue.isNotEmpty() && results.size < 2000) {
            val currentDocId = queue.removeFirst()
            if (!visited.add(currentDocId)) continue

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
            try {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext() && results.size < 2000) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null

                        if (docId.isNullOrBlank()) continue

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(docId)
                        } else if (isAudioFile(name, mime)) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            results.add(
                                DiscoveredAudioFile(
                                    uri = fileUri,
                                    displayName = name?.ifBlank { "Audio Track" } ?: "Audio Track"
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Skip inaccessible subfolders cleanly.
            }
        }
        return results
    }

    fun resolveDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return@use cursor.getString(idx)
                    }
                    null
                } ?: uri.lastPathSegment ?: "Local Audio"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Local Audio"
        }
    }

    fun readMetadata(context: Context, uri: Uri): AudioFileMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotEmpty() }
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()?.takeIf { it.isNotEmpty() }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()?.takeIf { it.isNotEmpty() }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            AudioFileMetadata(title = title, artist = artist, album = album, durationMs = durationMs)
        } catch (_: Exception) {
            AudioFileMetadata(title = null, artist = null, album = null, durationMs = 0L)
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun buildCandidate(context: Context, uri: Uri, knownName: String? = null): AudioImportCandidate {
        val displayName = knownName?.takeIf { it.isNotBlank() } ?: resolveDisplayName(context, uri)
        val metadata = readMetadata(context, uri)
        return AudioImportCandidate(uri = uri, displayName = displayName, metadata = metadata)
    }

    private fun isAudioFile(name: String?, mime: String?): Boolean {
        if (mime != null && mime.startsWith("audio/", ignoreCase = true)) return true
        val ext = name?.substringAfterLast('.', "")?.lowercase() ?: return false
        return audioExtensions.contains(ext)
    }
}
