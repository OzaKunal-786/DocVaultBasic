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
import org.tukaani.xz.XZOutputStream
import java.io.OutputStream

data class BackupData(val documents: List<DocumentEntity>, val folders: List<FolderEntity>)

class BackupManager(
    private val context: Context,
    private val documentRepository: DocumentRepository,
    private val folderRepository: FolderRepository,
    private val encryptionManager: EncryptionManager
) {

    suspend fun createBackup(uri: Uri) = withContext(Dispatchers.IO) {
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
    }

    // Restore logic would go here
}
