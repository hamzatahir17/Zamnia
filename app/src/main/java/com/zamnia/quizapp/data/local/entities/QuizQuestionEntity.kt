package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = DownloadedPackageEntity::class,
            parentColumns = ["packageId"],
            childColumns = ["packageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuizQuestionEntity(
    @PrimaryKey
    val id: String,
    val packageId: String,
    val classLevel: Int,
    val subject: String,
    val chapter: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: Int
)
