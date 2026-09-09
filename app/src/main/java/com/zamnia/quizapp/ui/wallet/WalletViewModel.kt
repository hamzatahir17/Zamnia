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

    private val _remainingTransfers = MutableStateFlow(2)
    val remainingTransfers: StateFlow<Int> = _remainingTransfers.asStateFlow()

    init {
        // Automatically refresh on init
        refreshWallet()
    }

    fun refreshWallet() {
        viewModelScope.launch {
            // Refresh daily limit from Supabase
            val count = repository.getDailyTransferCount()
            _remainingTransfers.value = (2 - count).coerceAtLeast(0)
            
            // Refresh user profile for latest coins
            repository.getUserProfile()
        }
    }

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
            val response = repository.transferCoins(publicId, amount)
            
            when (response) {
                "SUCCESS" -> {
                    _transferState.value = TransferState.Success
                    refreshWallet() // Automatically refresh everything after success
                }
                "DAILY_LIMIT_REACHED" -> {
                    _transferState.value = TransferState.Error("Daily limit of 2 transfers reached.")
                }
                "INSUFFICIENT_FUNDS" -> {
                    _transferState.value = TransferState.Error("You don't have enough coins.")
                }
                "INVALID_RECIPIENT" -> {
                    _transferState.value = TransferState.Error("Friend ID not found.")
                }
                "SAME_USER" -> {
                    _transferState.value = TransferState.Error("You cannot send coins to yourself.")
                }
                else -> {
                    _transferState.value = TransferState.Error("Transfer failed: $response")
                }
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
