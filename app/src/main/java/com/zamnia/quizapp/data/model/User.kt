package com.zamnia.quizapp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    val uid: String = "",
    @SerialName("user_id")
    val userId: String = "", 
    val email: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("coin_balance")
    val coinBalance: Long = 0,
    @SerialName("unlocked_themes")
    val unlockedThemes: List<String> = listOf("default"),
    @SerialName("active_theme_id")
    val activeThemeId: String = "default",
    @SerialName("last_custom_timer")
    val lastCustomTimer: Int = 30
)
