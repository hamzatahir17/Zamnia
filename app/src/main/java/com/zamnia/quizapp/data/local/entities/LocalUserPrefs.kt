package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_prefs")
data class LocalUserPrefs(
    @PrimaryKey val userId: String,
    val lastCustomTimer: Int,
    val activeThemeId: String
)
