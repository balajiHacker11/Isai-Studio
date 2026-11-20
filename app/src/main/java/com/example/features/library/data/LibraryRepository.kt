package com.example.features.library.data

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.model.LikedTrack
import com.example.core.model.PlayHistoryEntry
import com.example.core.model.Track
import com.example.core.model.UserPlaylist
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LibraryRepository(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth?
) {
    private fun requireVerifiedUid(expectedUserId: String): String {
        val uid = auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
        check(uid == expectedUserId) { "Authenticated user mismatch." }
        return uid
    }

    fun observeLikedTracks(userId: String): Flow<List<LikedTrack>> {
        val firestore = db ?: return flowOf(emptyList())
        return firestore.collection("users").document(userId).collection("liked_tracks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LikedTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    fun observePlaylists(userId: String): Flow<List<UserPlaylist>> {
        val firestore = db ?: return flowOf(emptyList())
        return firestore.collection("users").document(userId).collection("playlists")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserPlaylist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    fun observePlayHistory(userId: String): Flow<List<PlayHistoryEntry>> {
        val firestore = db ?: return flowOf(emptyList())
        return firestore.collection("users").document(userId).collection("play_history")
            .orderBy("playedAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlayHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    suspend fun toggleLike(userId: String, track: Track, currentlyLiked: Boolean): Result<Boolean> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val docId = "track_${track.id}"
        val path = "users/$userId/liked_tracks/$docId"
        return try {
            val uid = requireVerifiedUid(userId)
            val docRef = firestore.collection("users").document(uid).collection("liked_tracks").document(docId)
            if (currentlyLiked) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "trackNumber" to track.id.toLong(),
                    "title" to track.title.take(160).ifBlank { "Track" },
                    "album" to track.album.take(160).ifBlank { "Album" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (currentlyLiked) OperationType.DELETE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String,
        coverKey: String = "artist_mic_studio",
        initialTrackIds: List<Int> = emptyList()
    ): Result<String> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val cleanName = name.trim().take(80)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        val playlistId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireVerifiedUid(userId)
            val docRef = firestore.collection("users").document(uid).collection("playlists").document(playlistId)
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "description" to description.trim().take(240),
                "trackIds" to initialTrackIds.distinct().take(200).map { it.toLong() },
                "coverKey" to coverKey.take(40).ifBlank { "artist_mic_studio" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(playlistId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistTracks(userId: String, playlistId: String, trackIds: List<Long>): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(playlistId.isNotBlank())
            firestore.collection("users").document(uid).collection("playlists").document(playlistId)
                .update(
                    mapOf(
                        "trackIds" to trackIds.distinct().take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun renamePlaylist(userId: String, playlistId: String, newName: String, newDescription: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val cleanName = newName.trim().take(80)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireVerifiedUid(userId)
            firestore.collection("users").document(uid).collection("playlists").document(playlistId)
                .update(
                    mapOf(
                        "name" to cleanName,
                        "description" to newDescription.trim().take(240),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val path = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireVerifiedUid(userId)
            firestore.collection("users").document(uid).collection("playlists").document(playlistId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordPlayHistory(userId: String, track: Track): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val entryId = "hist_${System.currentTimeMillis()}_${track.id}"
        val path = "users/$userId/play_history/$entryId"
        return try {
            val uid = requireVerifiedUid(userId)
            val payload = mapOf(
                "userId" to uid,
                "trackNumber" to track.id.toLong(),
                "title" to track.title.take(160).ifBlank { "Track" },
                "album" to track.album.take(160).ifBlank { "Album" },
                "playedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid).collection("play_history").document(entryId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
