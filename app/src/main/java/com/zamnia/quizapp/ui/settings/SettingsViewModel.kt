package com.zamnia.quizapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.local.entities.LocalQuizHistory
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.data.model.User
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository
    private val client = ZamniaEngine.supabase

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _quizHistory = MutableStateFlow<List<LocalQuizHistory>>(emptyList())
    val quizHistory: StateFlow<List<LocalQuizHistory>> = _quizHistory.asStateFlow()

    private val _availableThemes = MutableStateFlow<List<Theme>>(emptyList())
    val availableThemes: StateFlow<List<Theme>> = _availableThemes.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _userProfile.value = repository.getUserProfile()
            _availableThemes.value = repository.getThemes()
            repository.getAllQuizHistory().collect {
                _quizHistory.value = it
            }
        }
    }

    fun purchaseTheme(themeId: String, price: Long) {
        viewModelScope.launch {
            val result = repository.purchaseTheme(themeId, price)
            if (result.isSuccess) {
                _userProfile.value = repository.getUserProfile() // Refresh profile
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            client.auth.signOut()
        }
    }
}
