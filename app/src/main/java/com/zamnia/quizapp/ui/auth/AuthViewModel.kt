package com.zamnia.quizapp.ui.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zamnia.quizapp.BuildConfig
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class AuthViewModel : ViewModel() {
    private val client = ZamniaEngine.supabase
    private val repository = ZamniaEngine.repository

    val isOnline: StateFlow<Boolean> = ZamniaEngine.networkObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userProfile: StateFlow<User?> = repository.getUserProfileStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Monitor session status for debugging
        viewModelScope.launch {
            client.auth.sessionStatus.collect { status ->
                Log.d("AuthViewModel", "Session Status Changed: $status")
            }
        }
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut() // Force reset cached project tokens
        return googleSignInClient.signInIntent
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Capture guest profile before signing into Google
                val guestProfileBeforeAuth = repository.getAnyLocalUser()

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken ?: throw Exception("No ID Token received from Google")

                Log.d("AuthViewModel", "Google ID Token received successfully, signing into Supabase...")

                client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                }

                val newUid = client.auth.currentUserOrNull()?.id ?: ""
                val email = client.auth.currentUserOrNull()?.email ?: ""
                val realName = account.displayName 
                    ?: email.substringBefore("@").takeIf { it.isNotEmpty() }
                    ?: "Explorer"

                if (guestProfileBeforeAuth != null && guestProfileBeforeAuth.isGuest) {
                    Log.d("AuthViewModel", "Migrating Guest Account (${guestProfileBeforeAuth.uid}) data to new Google UID $newUid ($email)...")
                    
                    val migratedUser = User(
                        uid = newUid,
                        userId = null,
                        email = email,
                        displayName = realName,
                        coinBalance = guestProfileBeforeAuth.coinBalance,
                        activeThemeId = guestProfileBeforeAuth.activeThemeId,
                        unlockedThemes = guestProfileBeforeAuth.unlockedThemes
                    )
                    repository.migrateGuestToUser(guestProfileBeforeAuth.uid, migratedUser)
                } else {
                    val existingRemote = repository.getUserProfile()
                    if (existingRemote == null) {
                        val user = User(
                            uid = newUid,
                            userId = null, 
                            email = email,
                            displayName = realName,
                            coinBalance = 0L
                        )
                        repository.saveUserProfile(user)
                    }
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In Error: ${e.message}", e)
                val friendlyMessage = when {
                    e is ApiException -> "Google Sign-In error (Code ${e.statusCode})"
                    e.message?.contains("network", ignoreCase = true) == true -> "No internet connection"
                    else -> "Authentication failed: ${e.localizedMessage}"
                }
                _authState.value = AuthState.Error(friendlyMessage)
            }
        }
    }

    fun signInWithGoogle(context: Context, onFallback: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            Log.d("AuthViewModel", "Starting Google Sign-In with Client ID: $clientId")

            if (clientId.isBlank()) {
                Log.w("AuthViewModel", "GOOGLE_WEB_CLIENT_ID is empty, falling back to GoogleSignIn Intent...")
                onFallback()
                return@launch
            }

            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetSignInWithGoogleOption.Builder(clientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                val googleIdTokenCredential = (credential as? GoogleIdTokenCredential)
                    ?: try {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to parse GoogleIdTokenCredential: ${e.message}")
                        null
                    }

                if (googleIdTokenCredential != null) {
                    Log.d("AuthViewModel", "Google ID Token received via CredentialManager, signing into Supabase...")
                    
                    // Capture guest profile before signing into Google
                    val guestProfileBeforeAuth = repository.getAnyLocalUser()

                    try {
                        client.auth.signInWith(IDToken) {
                            idToken = googleIdTokenCredential.idToken
                            provider = Google
                        }
                    } catch (supabaseError: Exception) {
                        Log.e("AuthViewModel", "Supabase Auth Error: ${supabaseError.message}", supabaseError)
                        _authState.value = AuthState.Error("Supabase error: ${supabaseError.localizedMessage}")
                        return@launch
                    }
                    
                    val newUid = client.auth.currentUserOrNull()?.id ?: ""
                    val email = client.auth.currentUserOrNull()?.email ?: ""
                    val realName = googleIdTokenCredential.displayName 
                        ?: email.substringBefore("@").takeIf { it.isNotEmpty() }
                        ?: "Explorer"
                    
                    if (guestProfileBeforeAuth != null && guestProfileBeforeAuth.isGuest) {
                        Log.d("AuthViewModel", "Migrating Guest Account (${guestProfileBeforeAuth.uid}) data to new Google UID $newUid ($email)...")
                        
                        val migratedUser = User(
                            uid = newUid,
                            userId = null,
                            email = email,
                            displayName = realName,
                            coinBalance = guestProfileBeforeAuth.coinBalance,
                            activeThemeId = guestProfileBeforeAuth.activeThemeId,
                            unlockedThemes = guestProfileBeforeAuth.unlockedThemes
                        )
                        repository.migrateGuestToUser(guestProfileBeforeAuth.uid, migratedUser)
                    } else {
                        val existingRemote = repository.getUserProfile()
                        if (existingRemote == null) {
                            val user = User(
                                uid = newUid,
                                userId = null, 
                                email = email,
                                displayName = realName,
                                coinBalance = 0L
                            )
                            repository.saveUserProfile(user)
                        }
                    }
                    _authState.value = AuthState.Success
                } else {
                    Log.w("AuthViewModel", "CredentialManager returned unknown format, invoking fallback...")
                    onFallback()
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "CredentialManager exception: ${e.message}, invoking fallback...")
                onFallback()
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
                        userId = null, 
                        email = "guest@zamnia.com",
                        displayName = "Guest Explorer",
                        coinBalance = 0L,
                        activeThemeId = "default",
                        unlockedThemes = listOf("default")
                    )
                    repository.saveUserProfile(guestUser)
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Guest Login Error: ${e.message}", e)
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
    suspend fun validateSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthViewModel", "Validating session (Offline-First Persistent Check)...")

            // 1. Give Supabase Auth up to 2000ms to load session from encrypted storage
            withTimeoutOrNull(2000) {
                client.auth.sessionStatus.first { status ->
                    status is SessionStatus.Authenticated || (status is SessionStatus.NotAuthenticated && status.isSignOut)
                }
            }

            val currentUser = client.auth.currentUserOrNull()

            // 2. Auto-refresh session if online
            if (currentUser != null) {
                try {
                    client.auth.refreshCurrentSession()
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Session auto-refresh skipped or failed (offline mode): ${e.message}")
                }
            }

            // 3. Check local Room DB cache
            val hasLocalUser = repository.hasLocalUserSession()

            Log.d("AuthViewModel", "Session check: CurrentUser=${currentUser?.id}, HasLocalUser=$hasLocalUser")

            // 4. Determine persistent login state:
            if (currentUser != null) {
                val isRemoteValid = repository.verifyRemoteSession()
                if (!isRemoteValid) {
                    Log.w("AuthViewModel", "Remote user was deleted from database! Signing out...")
                    repository.clearAllLocalData()
                    try { client.auth.signOut() } catch (e: Exception) { }
                    return@withContext false
                }
                return@withContext true
            }

            if (hasLocalUser) {
                return@withContext true
            }

            Log.d("AuthViewModel", "No active session or local user found, redirecting to Onboarding")
            return@withContext false
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error during session validation: ${e.message}", e)
            return@withContext repository.hasLocalUserSession()
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Clear local data on background thread BEFORE signing out
                repository.clearAllLocalData()
                
                // Sign out from Supabase
                client.auth.signOut()
                
                // Small delay to ensure the session is fully cleared before UI updates
                delay(500.milliseconds)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Logout error: ${e.message}")
            } finally {
                // Set explicitly to LoggedOut to trigger navigation in UI
                _authState.value = AuthState.LoggedOut
            }
        }
    }

    fun clearError() {
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}
