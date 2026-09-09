package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Pack(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("class_level")
    val classLevel: Int,
    @SerialName("question_count")
    val questionCount: String,
    @SerialName("icon_name")
    val iconName: String = "Science",
    @SerialName("color_hex")
    val colorHex: String = "#6200EE"
)
