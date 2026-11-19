package com.example.features.settings.data

import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.model.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PreferencesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun observePreferences(userId: String): Flow<UserPreferences> =
        db.collection("users").document(userId)
            .collection("preferences").document("settings")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserPreferences::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserPreferences(userId = userId)
                } else {
                    UserPreferences(userId = userId)
                }
            }

    suspend fun savePreferences(userId: String, prefs: UserPreferences): Result<Unit> {
        val path = "users/$userId/preferences/settings"
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("User must be signed in with Google.")
            check(uid == userId)
            val docRef = db.collection("users").document(uid)
                .collection("preferences").document("settings")
            val payload = prefs.toFirestoreMap(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = payload.filterKeys { it != "userId" }
                docRef.update(updatePayload).await()
            } else {
                docRef.set(payload).await()
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
