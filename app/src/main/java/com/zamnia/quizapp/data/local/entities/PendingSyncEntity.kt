package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val quizId: String,
    val score: Int,
    val earnedCoins: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
