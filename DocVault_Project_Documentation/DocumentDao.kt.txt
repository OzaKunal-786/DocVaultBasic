package com.docvaultbasic.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY dateAdded DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): DocumentEntity?

    @Query("SELECT * FROM documents WHERE fileName LIKE '%' || :query || '%' OR folderSource LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE folderSource = :folderPath")
    fun getDocumentsByFolder(folderPath: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY dateAdded DESC LIMIT 5")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT SUM(fileSize) FROM documents")
    fun getTotalSize(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT * FROM documents WHERE checksum = :checksum LIMIT 1")
    suspend fun getDocumentByChecksum(checksum: String): DocumentEntity?
}
