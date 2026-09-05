package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Pack(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    @SerialName("class_level")
    val classLevel: Int = 9,
    @SerialName("question_count")
    val questionCount: String = "20 Qs",
    @SerialName("icon_name")
    val iconName: String = "Science",
    @SerialName("color_hex")
    val colorHex: String = "#4CAF50",
    @SerialName("is_premium")
    val isPremium: Boolean = false,
    @SerialName("pdf_url")
    val pdfUrl: String? = null,
    @SerialName("index_order")
    val indexOrder: Int = 0
)
