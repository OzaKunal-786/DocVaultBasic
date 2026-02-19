package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.scale
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

    // Process bitmap with all adjustments: brightness, contrast, rotation, and sharpness
    fun processBitmap(
        original: Bitmap?, 
        brightness: Float, 
        contrast: Float, 
        rotation: Float,
        sharpness: Float // NEW: Sharpness parameter
    ): Bitmap {
        if (original == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        // STEP 1: Rotate the image
        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

        // STEP 2: Apply brightness and contrast
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        val withContrast = Bitmap.createBitmap(rotated.width, rotated.height, rotated.config)
        val canvas = Canvas(withContrast)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(rotated, 0f, 0f, paint)
        
        // STEP 3: Apply sharpness (if sharpness > 0)
        return if (sharpness > 0f) {
            applySharpen(withContrast, sharpness)
        } else {
            withContrast
        }
    }

    // NEW: Smart Crop function - automatically detects and crops document edges
    fun smartCrop(bitmap: Bitmap): Bitmap {
        // This is a simplified version - it crops 5% from each edge
        // A full CamScanner-style crop would need edge detection (complex)
        val cropPercent = 0.05f
        val cropX = (bitmap.width * cropPercent).toInt()
        val cropY = (bitmap.height * cropPercent).toInt()
        
        val newWidth = bitmap.width - (cropX * 2)
        val newHeight = bitmap.height - (cropY * 2)
        
        return if (newWidth > 0 && newHeight > 0) {
            Bitmap.createBitmap(bitmap, cropX, cropY, newWidth, newHeight)
        } else {
            bitmap
        }
    }

    // NEW: Apply sharpening effect to make image clearer
    private fun applySharpen(bitmap: Bitmap, intensity: Float): Bitmap {
        // Create a simple sharpen kernel
        // This makes edges sharper and clearer (like CamScanner)
        val scale = intensity * 0.1f // Scale down the intensity
        
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, bitmap.config)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val sharpened = IntArray(width * height)
        
        // Simple sharpening algorithm
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                
                // Get surrounding pixels
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]
                val center = pixels[index]
                
                // Sharpen each color channel
                val r = sharpenChannel(android.graphics.Color.red(center),
                    android.graphics.Color.red(top), android.graphics.Color.red(bottom),
                    android.graphics.Color.red(left), android.graphics.Color.red(right), scale)
                
                val g = sharpenChannel(android.graphics.Color.green(center),
                    android.graphics.Color.green(top), android.graphics.Color.green(bottom),
                    android.graphics.Color.green(left), android.graphics.Color.green(right), scale)
                
                val b = sharpenChannel(android.graphics.Color.blue(center),
                    android.graphics.Color.blue(top), android.graphics.Color.blue(bottom),
                    android.graphics.Color.blue(left), android.graphics.Color.blue(right), scale)
                
                sharpened[index] = android.graphics.Color.rgb(r, g, b)
            }
        }
        
        // Copy edge pixels directly
        for (x in 0 until width) {
            sharpened[x] = pixels[x]
            sharpened[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            sharpened[y * width] = pixels[y * width]
            sharpened[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }
        
        result.setPixels(sharpened, 0, width, 0, 0, width, height)
        return result
    }

    // Helper function to sharpen a single color channel
    private fun sharpenChannel(center: Int, top: Int, bottom: Int, left: Int, right: Int, scale: Float): Int {
        val sum = (top + bottom + left + right) / 4
        val sharpened = center + ((center - sum) * scale).toInt()
        return sharpened.coerceIn(0, 255)
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
