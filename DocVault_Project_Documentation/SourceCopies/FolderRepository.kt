package com.docvaultbasic.data.repository

import com.docvaultbasic.data.database.FolderDao
import com.docvaultbasic.data.database.FolderEntity
import kotlinx.coroutines.flow.Flow

class FolderRepository(private val folderDao: FolderDao) {
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val folderCount: Flow<Int> = folderDao.getFolderCount()

    suspend fun insertFolder(folder: FolderEntity) {
        folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    suspend fun getEnabledFolders(): List<FolderEntity> {
        return folderDao.getEnabledFolders()
    }
}
