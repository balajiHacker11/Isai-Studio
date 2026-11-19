package com.example.features.player.data

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.model.TrackLyrics
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LyricsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun observeTrackLyrics(userId: String, trackId: Int): Flow<TrackLyrics?> =
        db.collection("users").document(userId)
            .collection("lyrics").document("track_$trackId")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(TrackLyrics::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }

    suspend fun saveTrackLyrics(userId: String, trackId: Int, lrcContent: String): Result<Unit> {
        val cleanContent = lrcContent.trim().take(15000)
        if (cleanContent.isEmpty()) {
            return Result.failure(IllegalArgumentException("Lyrics content cannot be empty"))
        }
        val docId = "track_$trackId"
        val path = "users/$userId/lyrics/$docId"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("User must be signed in with Google.")
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("lyrics").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "lrcContent" to cleanContent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "trackNumber" to trackId.toLong(),
                        "lrcContent" to cleanContent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
