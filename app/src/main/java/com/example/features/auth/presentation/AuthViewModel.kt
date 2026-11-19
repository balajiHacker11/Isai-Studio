package com.example.features.auth.presentation

import android.app.Activity
import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.local.LocalAudioStore
import com.example.features.auth.data.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val localAudioStore: LocalAudioStore
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.authStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = authRepository.currentUser()
        )

    val introCompleted: StateFlow<Boolean?> = localAudioStore.introCompletedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var silentSignInAttempted = false

    fun attemptSilentSignInIfNeeded(context: Context) {
        if (silentSignInAttempted || authRepository.currentUser() != null) return
        silentSignInAttempted = true
        viewModelScope.launch {
            authRepository.attemptSilentSignIn(context)
        }
    }

    fun signInWithGoogle(activity: Activity) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInInteractive(activity)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                },
                onFailure = { error ->
                    val message = if (error is GetCredentialCancellationException) {
                        null
                    } else {
                        error.localizedMessage ?: "Google Sign-In failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
            )
        }
    }

    fun completeIntro() {
        viewModelScope.launch {
            localAudioStore.setIntroCompleted(true)
        }
    }

    fun replayIntro() {
        viewModelScope.launch {
            localAudioStore.setIntroCompleted(false)
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository.signOut(context)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
