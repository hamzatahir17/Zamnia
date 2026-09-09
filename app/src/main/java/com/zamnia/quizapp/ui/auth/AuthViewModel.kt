package com.zamnia.quizapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.BuildConfig
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.Google

class AuthViewModel : ViewModel() {
    private val client = ZamniaEngine.supabase
    private val repository = ZamniaEngine.repository

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    client.auth.signInWith(IDToken) {
                        idToken = credential.idToken
                        // Use the Google provider directly
                        this.provider = Google
                    }
                    
                    val uid = client.auth.currentUserOrNull()?.id ?: ""
                    
                    // If profile doesn't exist, initialize it
                    val existingProfile = repository.getUserProfile()
                    if (existingProfile == null) {
                        val user = User(
                            uid = uid,
                            userId = (100000..999999).random().toString(),
                            email = client.auth.currentUserOrNull()?.email ?: "",
                            displayName = "Explorer",
                            coinBalance = 0L
                        )
                        repository.saveUserProfile(user)
                    }
                    _authState.value = AuthState.Success
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Google Sign-In Error: ${e.message}", e)
                val friendlyMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> "No internet connection"
                    e.message?.contains("cancel", ignoreCase = true) == true -> "Sign-in cancelled"
                    else -> "Authentication failed. Please try again."
                }
                _authState.value = AuthState.Error(friendlyMessage)
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                client.auth.signInAnonymously()
                val uid = client.auth.currentUserOrNull()?.id ?: ""
                
                val existingProfile = repository.getUserProfile()
                if (existingProfile == null) {
                    val guestUser = User(
                        uid = uid,
                        userId = (100000..999999).random().toString(),
                        email = "guest@zamnia.com",
                        displayName = "Guest Explorer",
                        coinBalance = 0L
                    )
                    repository.saveUserProfile(guestUser)
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Guest Login Error: ${e.message}", e)
                val friendlyMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> "No internet connection"
                    else -> "Guest login failed. Please try again."
                }
                _authState.value = AuthState.Error(friendlyMessage)
            }
        }
    }

    fun isUserLoggedIn(): Boolean = client.auth.currentUserOrNull() != null

    /**
     * Validates the current session against the backend.
     * Uses Dispatchers.IO to ensure no UI thread freezing.
     */
    suspend fun validateSession(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (client.auth.currentUserOrNull() == null) return@withContext false
        
        return@withContext try {
            // Force verify against Remote Supabase Database
            val isRemoteValid = repository.verifyRemoteSession()
            if (!isRemoteValid) {
                client.auth.signOut()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Network error during validation: ${e.message}")
            true 
        }
    }

    fun logout() {
        viewModelScope.launch {
            client.auth.signOut()
            _authState.value = AuthState.Idle
        }
    }

    fun clearError() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
