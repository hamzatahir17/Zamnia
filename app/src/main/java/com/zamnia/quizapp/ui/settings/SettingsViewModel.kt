package com.zamnia.quizapp.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.local.entities.LocalQuizHistory
import com.zamnia.quizapp.data.model.Theme
import com.zamnia.quizapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    val userProfile: StateFlow<User?> = repository.getUserProfileStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _quizHistory = MutableStateFlow<List<LocalQuizHistory>>(emptyList())
    val quizHistory: StateFlow<List<LocalQuizHistory>> = _quizHistory.asStateFlow()

    private val _availableThemes = MutableStateFlow<List<Theme>>(emptyList())
    val availableThemes: StateFlow<List<Theme>> = _availableThemes.asStateFlow()

    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _availableThemes.value = repository.getThemes()
            } catch (e: Exception) {
                Log.e("SettingsVM", "Themes load failed: ${e.message}")
            }

            repository.getAllQuizHistory().collect {
                _quizHistory.value = it
            }
        }
    }

    fun purchaseTheme(themeId: String, price: Long) {
        viewModelScope.launch {
            val result = repository.purchaseTheme(themeId, price)
            if (result.isFailure) {
                _purchaseMessage.value = result.exceptionOrNull()?.message ?: "Purchase failed"
            } else {
                _purchaseMessage.value = "Theme unlocked successfully!"
            }
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            val result = repository.selectTheme(themeId)
            if (result.isFailure) {
                _purchaseMessage.value = result.exceptionOrNull()?.message ?: "Theme selection failed"
            }
        }
    }

    fun clearPurchaseMessage() {
        _purchaseMessage.value = null
    }
}
