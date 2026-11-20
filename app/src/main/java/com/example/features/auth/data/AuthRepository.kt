package com.example.features.auth.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val db: FirebaseFirestore?,
    private val auth: FirebaseAuth?
) {
    val isFirebaseConfigured: Boolean
        get() = db != null && auth != null

    fun authStateFlow(): Flow<FirebaseUser?> {
        val firebaseAuth = auth ?: return flowOf(null)
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { a ->
                trySend(a.currentUser)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    fun currentUser(): FirebaseUser? = auth?.currentUser

    suspend fun attemptSilentSignIn(context: Context): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.success(null)
        if (db == null) return Result.success(null)
        val existing = firebaseAuth.currentUser
        if (existing != null) return Result.success(existing)

        val clientId = resolveWebClientId(context)
            ?: return Result.success(null)

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credential = credentialManager.getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val result = firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                result.user?.let { ensureUserProfile(it) }
                Result.success(result.user)
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.success(null)
        }
    }

    suspend fun signInInteractive(activity: Activity): Result<FirebaseUser> {
        val firebaseAuth = auth
        if (firebaseAuth == null || db == null) {
            return Result.failure(
                IllegalStateException(
                    "Firebase is not configured in this APK build. Include app/google-services.json (or set GOOGLE_SERVICES_JSON_BASE64 in GitHub Secrets) and rebuild."
                )
            )
        }
        val clientId = resolveWebClientId(activity)
            ?: return Result.failure(
                IllegalStateException(
                    "Google Sign-In configuration missing (default_web_client_id). Include app/google-services.json in this build."
                )
            )

        return try {
            val credentialManager = CredentialManager.create(activity)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authResult = firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                val user = authResult.user ?: return Result.failure(IllegalStateException("Firebase user null after sign-in."))
                ensureUserProfile(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type returned."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val path = "users/${user.uid}"
        return try {
            val docRef = firestore.collection("users").document(user.uid)
            val snapshot = docRef.get().await()
            val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Isai Listener").take(80)
            val email = (user.email ?: "").take(160)
            val photoUrl = user.photoUrl?.toString()?.take(500)

            if (!snapshot.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to user.uid,
                    "displayName" to displayName,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    createPayload["photoUrl"] = photoUrl
                }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "email" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    updatePayload["photoUrl"] = photoUrl
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                auth?.signOut()
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
