package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users_local")
data class UserEntity(
    @PrimaryKey
    val userId: String, // Supabase User ID
    val name: String,
    val email: String,
    val coins: Long,
    val activeThemeId: String,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
