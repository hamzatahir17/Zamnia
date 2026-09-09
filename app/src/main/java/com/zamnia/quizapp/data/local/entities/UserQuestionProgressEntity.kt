package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.ForeignKey

@Entity(
    tableName = "user_question_progress",
    foreignKeys = [
        ForeignKey(
            entity = DownloadedPackageEntity::class,
            parentColumns = ["packageId"],
            childColumns = ["packageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserQuestionProgressEntity(
    @PrimaryKey
    val questionId: String,
    val packageId: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
