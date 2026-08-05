package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val amount: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "transfer" // "transfer", "quiz_reward", "purchase"
)
