package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_history")
data class PdfHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val uriString: String,
    val actionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L
)
