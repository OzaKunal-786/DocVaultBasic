package com.docvaultbasic.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderPath: String,
    val folderName: String,
    val isEnabled: Boolean = true,
    val lastScanned: Long = 0,
    val documentCount: Int = 0
)
