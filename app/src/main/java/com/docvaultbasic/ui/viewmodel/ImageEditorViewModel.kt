package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class ImageEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun processBitmap(original: Bitmap?, brightness: Float, contrast: Float, rotation: Float): Bitmap {
        if (original == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        val result = Bitmap.createBitmap(rotated.width, rotated.height, rotated.config)
        val canvas = Canvas(result)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        
        return result
    }

    fun saveAsPdf(bitmap: Bitmap, name: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            withContext(Dispatchers.IO) {
                try {
                    val vaultDir = File(context.filesDir, "vault")
                    if (!vaultDir.exists()) vaultDir.mkdirs()

                    val pdfDocument = PdfDocument()
                    
                    // Optimized Compression
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val compressedData = outputStream.toByteArray()
                    val compressedBitmap = android.graphics.BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)

                    val pageInfo = PdfDocument.PageInfo.Builder(compressedBitmap.width, compressedBitmap.height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)

                    // Clean filename logic
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
                        folderSource = "App Scanner",
                        isEncrypted = false,
                        checksum = calculateFileChecksum(file),
                        thumbnailPath = null
                    )
                    documentRepository.insertDocument(docEntity)
                    
                    withContext(Dispatchers.Main) {
                        _isSaving.value = false
                        onComplete()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _isSaving.value = false
                    }
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
