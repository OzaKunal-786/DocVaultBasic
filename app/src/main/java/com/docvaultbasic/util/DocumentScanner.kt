package com.docvaultbasic.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.data.repository.DocumentRepository
import com.docvaultbasic.data.repository.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class DocumentScanner(
    private val context: Context,
    private val documentRepository: DocumentRepository,
    private val folderRepository: FolderRepository
) {

    suspend fun scanDocuments() = withContext(Dispatchers.IO) {
        val folders = folderRepository.getEnabledFolders()
        folders.forEach { folder ->
            if (folder.folderPath.startsWith("content://")) {
                scanUriRecursive(folder.folderPath)
            } else {
                scanPathRecursive(File(folder.folderPath))
            }
        }
    }

    private suspend fun scanPathRecursive(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanPathRecursive(file)
            } else if (file.isFile && file.name.endsWith(".pdf", ignoreCase = true)) {
                processFile(file, directory.absolutePath)
            }
        }
    }

    private suspend fun scanUriRecursive(uriString: String) {
        val folderUri = Uri.parse(uriString)
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
        scanDocumentFileRecursive(documentFile, uriString)
    }

    private suspend fun scanDocumentFileRecursive(docFile: DocumentFile?, folderPath: String) {
        docFile?.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDocumentFileRecursive(file, folderPath)
            } else if (file.isFile && file.name?.endsWith(".pdf", ignoreCase = true) == true) {
                processDocumentFile(file, folderPath)
            }
        }
    }

    private suspend fun processFile(file: File, folderPath: String) {
        val uri = Uri.fromFile(file)
        val checksum = calculateChecksum(uri)
        if (checksum.isBlank()) return
        
        val existingDoc = documentRepository.getDocumentByChecksum(checksum)
        if (existingDoc == null) {
            val newDoc = DocumentEntity(
                fileName = file.name,
                originalPath = uri.toString(),
                storedPath = "",
                fileSize = file.length(),
                dateAdded = System.currentTimeMillis(),
                dateModified = file.lastModified(),
                folderSource = folderPath,
                isEncrypted = false,
                checksum = checksum,
                thumbnailPath = null
            )
            documentRepository.insertDocument(newDoc)
        }
    }

    private suspend fun processDocumentFile(file: DocumentFile, folderPath: String) {
        val checksum = calculateChecksum(file.uri)
        if (checksum.isBlank()) return
        
        val existingDoc = documentRepository.getDocumentByChecksum(checksum)
        if (existingDoc == null) {
            val newDoc = DocumentEntity(
                fileName = file.name ?: "Unknown",
                originalPath = file.uri.toString(),
                storedPath = "",
                fileSize = file.length(),
                dateAdded = System.currentTimeMillis(),
                dateModified = file.lastModified(),
                folderSource = folderPath,
                isEncrypted = false,
                checksum = checksum,
                thumbnailPath = null
            )
            documentRepository.insertDocument(newDoc)
        }
    }

    private fun calculateChecksum(uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
