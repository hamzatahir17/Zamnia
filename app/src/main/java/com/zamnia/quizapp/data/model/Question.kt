package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Question(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("package_id")
    val packageId: String = "",
    @SerialName("question")
    val question: String = "",
    @SerialName("options")
    val options: List<String> = emptyList(),
    @SerialName("correct_answer")
    val correctAnswer: Int = 0,
    @SerialName("category")
    val category: String = "General",
    @SerialName("difficulty")
    val difficulty: String = "Easy"
) {
    // Convenience property for UI compatibility
    val correctAnswerIndex: Int
        get() = correctAnswer
}
