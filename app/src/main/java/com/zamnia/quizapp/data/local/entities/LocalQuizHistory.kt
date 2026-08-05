package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class LocalQuizHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val score: Int,
    val totalQuestions: Int,
    val coinsEarned: Int,
    val timestamp: Long
)
