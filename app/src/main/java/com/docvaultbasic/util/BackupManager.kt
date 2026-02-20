package com.docvaultbasic.util

import android.content.Context
import android.net.Uri
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.data.database.FolderEntity
import com.docvaultbasic.data.repository.DocumentRepository
import com.docvaultbasic.data.repository.FolderRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZInputStream
import org.tukaani.xz.XZOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class BackupData(val documents: List<DocumentEntity>, val folders: List<FolderEntity>)

class BackupManager(
    private val context: Context,
    private val documentRepository: DocumentRepository,
    private val folderRepository: FolderRepository,
    private val encryptionManager: EncryptionManager
) {

    suspend fun createBackup(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val documents = documentRepository.allDocuments.first()
            val folders = folderRepository.allFolders.first()
            val backupData = BackupData(documents, folders)
            val json = Gson().toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val compressedStream = XZOutputStream(outputStream, LZMA2Options())
                val encryptedData = encryptionManager.encrypt(json.toByteArray())
                compressedStream.write(encryptedData)
                compressedStream.finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val compressedInputStream = XZInputStream(inputStream)
                val buffer = ByteArray(8192)
                val output = ByteArrayOutputStream()
                var bytesRead: Int
                while (compressedInputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                
                val encryptedData = output.toByteArray()
                val decryptedData = encryptionManager.decrypt(encryptedData)
                val json = String(decryptedData)
                
                val backupData = Gson().fromJson(json, BackupData::class.java)
                
                // Restore folders
                backupData.folders.forEach { folder ->
                    folderRepository.insertFolder(folder)
                }
                
                // Restore documents
                backupData.documents.forEach { document ->
                    documentRepository.insertDocument(document)
                }
                
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
