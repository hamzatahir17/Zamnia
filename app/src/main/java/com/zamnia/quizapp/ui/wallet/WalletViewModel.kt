package com.zamnia.quizapp.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WalletViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    private val _recipientUser = MutableStateFlow<User?>(null)
    val recipientUser: StateFlow<User?> = _recipientUser.asStateFlow()

    fun findRecipient(publicId: String) {
        if (publicId.length < 6) {
            _recipientUser.value = null
            return
        }
        viewModelScope.launch {
            _recipientUser.value = repository.getUserByPublicId(publicId)
        }
    }

    fun transferCoins(publicId: String, amount: Long) {
        viewModelScope.launch {
            _transferState.value = TransferState.Loading
            val result = repository.transferCoins(publicId, amount)
            if (result.isSuccess) {
                _transferState.value = TransferState.Success
            } else {
                _transferState.value = TransferState.Error(result.exceptionOrNull()?.message ?: "Transfer failed")
            }
        }
    }

    fun resetState() {
        _transferState.value = TransferState.Idle
    }
}

sealed class TransferState {
    object Idle : TransferState()
    object Loading : TransferState()
    object Success : TransferState()
    data class Error(val message: String) : TransferState()
}
