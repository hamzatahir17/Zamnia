package com.zamnia.quizapp.ui.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zamnia.quizapp.ZamniaEngine
import com.zamnia.quizapp.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel : ViewModel() {
    private val repository = ZamniaEngine.repository

    val userProfile: StateFlow<User?> = repository.getUserProfileStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _friendId = MutableStateFlow("")
    val friendId: StateFlow<String> = _friendId.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    private val _recipientUser = MutableStateFlow<User?>(null)
    val recipientUser: StateFlow<User?> = _recipientUser.asStateFlow()

    private val _remainingTransfers = MutableStateFlow(2)
    val remainingTransfers: StateFlow<Int> = _remainingTransfers.asStateFlow()

    init {
        viewModelScope.launch {
            userProfile.collect {
                refreshWallet()
            }
        }
    }

    fun updateFriendId(id: String) {
        val sanitized = id.take(6)
        _friendId.value = sanitized
        if (sanitized.length == 6) {
            findRecipient(sanitized)
        } else {
            _recipientUser.value = null
        }
    }

    fun updateAmount(amt: String) {
        _amount.value = amt
    }

    fun setMaxAmount() {
        val balance = userProfile.value?.coinBalance ?: 0L
        _amount.value = balance.toString()
    }

    fun refreshWallet() {
        viewModelScope.launch {
            val count = repository.getDailyTransferCount()
            _remainingTransfers.value = (2 - count).coerceAtLeast(0)
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

    fun transferCoins(publicId: String, coins: Long) {
        if (publicId.length < 6 || coins <= 0) return

        viewModelScope.launch {
            _transferState.value = TransferState.Loading
            val response = repository.transferCoins(publicId, coins)
            val cleanResponse = response.trim().removeSurrounding("\"").uppercase()
            
            Log.d("WalletViewModel", "transferCoins cleanResponse: '$cleanResponse'")

            if (cleanResponse == "SUCCESS") {
                _transferState.value = TransferState.Success
                refreshWallet()
                
                // Clear fields immediately on success
                _friendId.value = ""
                _amount.value = ""
                _recipientUser.value = null

                // Reset button state after brief confirmation animation
                delay(1500)
                _transferState.value = TransferState.Idle
            } else if (cleanResponse.contains("DAILY_LIMIT")) {
                _transferState.value = TransferState.Error("Daily limit of 2 transfers reached.")
            } else if (cleanResponse.contains("INSUFFICIENT")) {
                _transferState.value = TransferState.Error("You don't have enough coins.")
            } else if (cleanResponse.contains("RECIPIENT") || cleanResponse.contains("NOT_FOUND")) {
                _transferState.value = TransferState.Error("Friend ID not found.")
            } else if (cleanResponse.contains("SELF") || cleanResponse.contains("SAME_USER")) {
                _transferState.value = TransferState.Error("You cannot send coins to yourself.")
            } else {
                _transferState.value = TransferState.Error(cleanResponse.ifBlank { "Transfer failed." })
            }
        }
    }

    fun resetState() {
        _friendId.value = ""
        _amount.value = ""
        _transferState.value = TransferState.Idle
        _recipientUser.value = null
    }
}

sealed class TransferState {
    object Idle : TransferState()
    object Loading : TransferState()
    object Success : TransferState()
    data class Error(val message: String) : TransferState()
}
