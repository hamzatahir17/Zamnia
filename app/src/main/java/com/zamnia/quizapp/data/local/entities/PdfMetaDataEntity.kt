package com.zamnia.quizapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_metadata")
data class PdfMetaDataEntity(
    @PrimaryKey
    val id: String,
    val packageId: String,
    val title: String,
    val fileUrl: String,
    val localFilePath: String? = null
)
