package com.docvaultbasic.data.repository

import com.docvaultbasic.data.database.DocumentDao
import com.docvaultbasic.data.database.DocumentEntity
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = documentDao.getRecentDocuments()
    val totalSize: Flow<Long?> = documentDao.getTotalSize()
    val documentCount: Flow<Int> = documentDao.getDocumentCount()

    suspend fun insertDocument(document: DocumentEntity) {
        documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: DocumentEntity) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocument(document: DocumentEntity) {
        documentDao.deleteDocument(document)
    }

    suspend fun getDocumentById(id: Int): DocumentEntity? {
        return documentDao.getDocumentById(id)
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        return documentDao.searchDocuments(query)
    }

    fun getDocumentsByFolder(folderPath: String): Flow<List<DocumentEntity>> {
        return documentDao.getDocumentsByFolder(folderPath)
    }

    suspend fun getDocumentByChecksum(checksum: String): DocumentEntity? {
        return documentDao.getDocumentByChecksum(checksum)
    }
}
