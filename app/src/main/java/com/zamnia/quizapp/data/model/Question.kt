package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Question(
    val id: Long = 0,
    val question: String = "",
    val options: List<String> = emptyList(),
    @SerialName("correct_answer")
    val correctAnswerIndex: Int = 0,
    val category: String = "General"
)
