package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_packages")
data class DownloadedPackageEntity(
    @PrimaryKey
    val packageId: String, // e.g. "class9_physics_ch1"
    val classLevel: Int, // 9, 10, 11, 12
    val subject: String,
    val chapterName: String,
    val totalMcqs: Int,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis()
)
