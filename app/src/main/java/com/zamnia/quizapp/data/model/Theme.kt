package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Theme(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    @SerialName("cloudinary_url")
    val cloudinaryUrl: String = "",
    @SerialName("primary_color")
    val primaryColor: String = "",
    @SerialName("secondary_color")
    val secondaryColor: String = ""
)
