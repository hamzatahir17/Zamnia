package com.zamnia.quizapp.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    private val _availableThemes = MutableStateFlow<List<Theme>>(emptyList())
    val availableThemes: StateFlow<List<Theme>> = _availableThemes.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    init {
        loadThemes()
    }

    private fun loadThemes() {
        viewModelScope.launch {
            _availableThemes.value = repository.getThemes()
        }
    }

    fun purchaseTheme(themeId: String, price: Long) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            val result = repository.purchaseTheme(themeId, price)
            if (result.isSuccess) {
                _purchaseState.value = PurchaseState.Success
                loadThemes() // Refresh
            } else {
                _purchaseState.value = PurchaseState.Error(result.exceptionOrNull()?.message ?: "Purchase failed")
            }
        }
    }
}

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    object Success : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
