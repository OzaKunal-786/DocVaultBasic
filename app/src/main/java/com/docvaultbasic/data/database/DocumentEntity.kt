package com.docvaultbasic.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val originalPath: String,
    val storedPath: String,
    val fileSize: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val folderSource: String,
    val isEncrypted: Boolean,
    val checksum: String,
    val thumbnailPath: String?,
    val content: String? = null // Optional OCR text for searching
)
