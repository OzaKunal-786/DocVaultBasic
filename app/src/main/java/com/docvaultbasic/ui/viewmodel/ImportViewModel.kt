package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.data.repository.DocumentRepository
import com.docvaultbasic.util.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    fun saveDocumentDirectly(uri: Uri, name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val vaultDir = File(context.filesDir, "vault")
                    if (!vaultDir.exists()) vaultDir.mkdirs()

                    val pdfDocument = PdfDocument()
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    
                    if (bitmap != null) {
                        // High-performance JPEG compression
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val compressedData = outputStream.toByteArray()
                        val compressedBitmap = BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)

                        val pageInfo = PdfDocument.PageInfo.Builder(compressedBitmap.width, compressedBitmap.height, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                        pdfDocument.finishPage(page)

                        // Clean filename without extra numbers
                        val finalName = if (name.endsWith(".pdf", true)) name else "$name.pdf"
                        val file = File(vaultDir, finalName)
                        
                        FileOutputStream(file).use { pdfDocument.writeTo(it) }
                        pdfDocument.close()

                        val docEntity = DocumentEntity(
                            fileName = name,
                            originalPath = "file://${file.absolutePath}",
                            storedPath = file.absolutePath,
                            fileSize = file.length(),
                            dateAdded = System.currentTimeMillis(),
                            dateModified = System.currentTimeMillis(),
                            folderSource = "Imported",
                            isEncrypted = false,
                            checksum = calculateFileChecksum(file),
                            thumbnailPath = null
                        )
                        documentRepository.insertDocument(docEntity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun calculateFileChecksum(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { inputStream ->
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
