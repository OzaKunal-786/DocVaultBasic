package com.docvaultbasic.util

import android.content.Context
import android.net.Uri
import android.util.Log
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
    private val TAG = "DocumentScanner"
    private val supportedExtensions = listOf("pdf", "jpg", "jpeg", "png", "webp")

    suspend fun scanDocuments() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting scan...")
        val folders = folderRepository.getEnabledFolders()
        Log.d(TAG, "Scanning ${folders.size} enabled folders")
        
        folders.forEach { folder ->
            try {
                if (folder.folderPath.startsWith("content://")) {
                    Log.d(TAG, "Scanning URI: ${folder.folderPath}")
                    scanUriRecursive(folder.folderPath)
                } else {
                    Log.d(TAG, "Scanning Path: ${folder.folderPath}")
                    scanPathRecursive(File(folder.folderPath))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning folder: ${folder.folderPath}", e)
            }
        }
        Log.d(TAG, "Scan completed")
    }

    private suspend fun scanPathRecursive(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            Log.w(TAG, "Directory does not exist or is not a directory: ${directory.absolutePath}")
            return
        }

        directory.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    scanPathRecursive(file)
                } else if (file.isFile && isSupported(file.name)) {
                    processFile(file, directory.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing file: ${file.absolutePath}", e)
            }
        }
    }

    private suspend fun scanUriRecursive(uriString: String) {
        val folderUri = Uri.parse(uriString)
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
        if (documentFile == null || !documentFile.canRead()) {
            Log.w(TAG, "Cannot read from URI: $uriString")
            return
        }
        scanDocumentFileRecursive(documentFile, uriString)
    }

    private suspend fun scanDocumentFileRecursive(docFile: DocumentFile?, folderPath: String) {
        docFile?.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    scanDocumentFileRecursive(file, folderPath)
                } else if (file.isFile && isSupported(file.name ?: "")) {
                    processDocumentFile(file, folderPath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing DocumentFile: ${file.name}", e)
            }
        }
    }

    private fun isSupported(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return supportedExtensions.contains(ext)
    }

    private suspend fun processFile(file: File, folderPath: String) {
        val uri = Uri.fromFile(file)
        val checksum = calculateChecksum(uri)
        if (checksum.isBlank()) {
            Log.w(TAG, "Could not calculate checksum for: ${file.absolutePath}")
            return
        }
        
        val existingDoc = documentRepository.getDocumentByChecksum(checksum)
        if (existingDoc == null) {
            Log.d(TAG, "Adding new file: ${file.name}")
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
        if (checksum.isBlank()) {
            Log.w(TAG, "Could not calculate checksum for DocumentFile: ${file.name}")
            return
        }
        
        val existingDoc = documentRepository.getDocumentByChecksum(checksum)
        if (existingDoc == null) {
            Log.d(TAG, "Adding new DocumentFile: ${file.name}")
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
            Log.e(TAG, "Error calculating checksum for: $uri", e)
            ""
        }
    }
}
