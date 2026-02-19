package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
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
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

// Filter modes like CamScanner
enum class FilterMode {
    ORIGINAL,       // No filter
    MAGIC_COLOR,    // Enhanced with removed shadows
    GRAYSCALE,      // Gray mode
    BLACK_WHITE     // B&W mode
}

@HiltViewModel
class ImageEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    // FUNCTION 1: SMART EDGE DETECTION
    // This uses OpenCV to automatically find document corners
    fun detectDocumentEdges(bitmap: Bitmap): List<PointF> {
        return try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            // Convert to grayscale
            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 0.0)

            // Edge detection using Canny
            val edgesMat = Mat()
            Imgproc.Canny(grayMat, edgesMat, 75.0, 200.0)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                edgesMat,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            // Find the largest contour
            var maxContour: MatOfPoint? = null
            var maxArea = 0.0

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area > maxArea) {
                    maxArea = area
                    maxContour = contour
                }
            }

            // Approximate the contour to a polygon
            val corners = if (maxContour != null && maxArea > 1000) {
                val peri = Imgproc.arcLength(MatOfPoint2f(*maxContour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(
                    MatOfPoint2f(*maxContour.toArray()),
                    approx,
                    0.02 * peri,
                    true
                )

                val points = approx.toArray()
                
                // If we found 4 corners, use them
                if (points.size == 4) {
                    orderPoints(points.map { PointF(it.x.toFloat(), it.y.toFloat()) })
                } else {
                    // Fallback to full image corners
                    getDefaultCorners(bitmap.width, bitmap.height)
                }
            } else {
                // Fallback to full image corners
                getDefaultCorners(bitmap.width, bitmap.height)
            }

            // Clean up
            mat.release()
            grayMat.release()
            edgesMat.release()
            hierarchy.release()

            corners
        } catch (e: Exception) {
            // If OpenCV fails, return default corners
            getDefaultCorners(bitmap.width, bitmap.height)
        }
    }

    // Helper: Get default corners (full image)
    private fun getDefaultCorners(width: Int, height: Int): List<PointF> {
        return listOf(
            PointF(0f, 0f),                    // Top-left
            PointF(width.toFloat(), 0f),       // Top-right
            PointF(width.toFloat(), height.toFloat()), // Bottom-right
            PointF(0f, height.toFloat())       // Bottom-left
        )
    }

    // Helper: Order points in correct order (top-left, top-right, bottom-right, bottom-left)
    private fun orderPoints(points: List<PointF>): List<PointF> {
        val sorted = points.sortedBy { it.x + it.y }
        
        val topLeft = sorted[0]
        val bottomRight = sorted[3]
        
        val remaining = listOf(sorted[1], sorted[2])
        val topRight = if (remaining[0].x > remaining[1].x) remaining[0] else remaining[1]
        val bottomLeft = if (remaining[0].x < remaining[1].x) remaining[0] else remaining[1]
        
        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    // FUNCTION 2: PERSPECTIVE CORRECTION (DESKEW)
    // This straightens the document based on the selected corners
    fun applyPerspectiveCorrection(bitmap: Bitmap, corners: List<PointF>): Bitmap {
        return try {
            if (corners.size != 4) return bitmap

            val srcMat = Mat()
            Utils.bitmapToMat(bitmap, srcMat)

            // Source points (the corners user selected)
            val srcPoints = MatOfPoint2f(
                Point(corners[0].x.toDouble(), corners[0].y.toDouble()),
                Point(corners[1].x.toDouble(), corners[1].y.toDouble()),
                Point(corners[2].x.toDouble(), corners[2].y.toDouble()),
                Point(corners[3].x.toDouble(), corners[3].y.toDouble())
            )

            // Calculate destination size
            val width = maxOf(
                distance(corners[0], corners[1]),
                distance(corners[2], corners[3])
            ).toInt()

            val height = maxOf(
                distance(corners[0], corners[3]),
                distance(corners[1], corners[2])
            ).toInt()

            // Destination points (rectangle)
            val dstPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(width.toDouble(), 0.0),
                Point(width.toDouble(), height.toDouble()),
                Point(0.0, height.toDouble())
            )

            // Get perspective transform matrix
            val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

            // Apply transformation
            val dstMat = Mat()
            Imgproc.warpPerspective(
                srcMat,
                dstMat,
                transformMatrix,
                Size(width.toDouble(), height.toDouble())
            )

            // Convert back to bitmap
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dstMat, result)

            // Clean up
            srcMat.release()
            dstMat.release()
            transformMatrix.release()

            result
        } catch (e: Exception) {
            bitmap
        }
    }

    // Helper: Calculate distance between two points
    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    // FUNCTION 3: APPLY FILTERS
    // Apply CamScanner-style filter presets
    fun applyFilter(bitmap: Bitmap, mode: FilterMode): Bitmap {
        return when (mode) {
            FilterMode.ORIGINAL -> bitmap
            FilterMode.MAGIC_COLOR -> applyMagicColor(bitmap)
            FilterMode.GRAYSCALE -> applyGrayscale(bitmap)
            FilterMode.BLACK_WHITE -> applyBlackAndWhite(bitmap)
        }
    }

    // Magic Color filter - enhances document, removes shadows
    private fun applyMagicColor(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Enhance contrast and remove yellow/shadow tints
            val newR = enhanceChannel(r, 1.2f, 10)
            val newG = enhanceChannel(g, 1.2f, 10)
            val newB = enhanceChannel(b, 1.1f, 5)

            pixels[i] = Color.rgb(newR, newG, newB)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    // Grayscale filter
    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    // Black & White filter (high contrast)
    private fun applyBlackAndWhite(bitmap: Bitmap): Bitmap {
        val grayscale = applyGrayscale(bitmap)
        val result = grayscale.copy(grayscale.config, true)
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        // Apply threshold for B&W effect
        val threshold = 128
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i]) // Already grayscale, so R=G=B
            val bw = if (gray > threshold) 255 else 0
            pixels[i] = Color.rgb(bw, bw, bw)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    // Helper for channel enhancement
    private fun enhanceChannel(value: Int, contrast: Float, brightness: Int): Int {
        val enhanced = ((value - 128) * contrast + 128 + brightness).toInt()
        return enhanced.coerceIn(0, 255)
    }

    // FUNCTION 4: MANUAL ADJUSTMENTS
    // Process bitmap with manual controls (brightness, contrast, saturation, sharpness, rotation)
    fun processBitmap(
        original: Bitmap?,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        sharpness: Float,
        rotation: Float
    ): Bitmap {
        if (original == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        // Step 1: Rotate
        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

        // Step 2: Apply brightness, contrast, and saturation
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        cm.postConcat(satMatrix)

        val withAdjustments = Bitmap.createBitmap(rotated.width, rotated.height, rotated.config)
        val canvas = Canvas(withAdjustments)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(rotated, 0f, 0f, paint)

        // Step 3: Apply sharpness
        return if (sharpness > 0f) {
            applySharpen(withAdjustments, sharpness)
        } else {
            withAdjustments
        }
    }

    // Apply sharpening effect
    private fun applySharpen(bitmap: Bitmap, intensity: Float): Bitmap {
        val scale = intensity * 0.1f
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, bitmap.config)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val sharpened = IntArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]
                val center = pixels[index]
                
                val r = sharpenChannel(Color.red(center),
                    Color.red(top), Color.red(bottom),
                    Color.red(left), Color.red(right), scale)
                
                val g = sharpenChannel(Color.green(center),
                    Color.green(top), Color.green(bottom),
                    Color.green(left), Color.green(right), scale)
                
                val b = sharpenChannel(Color.blue(center),
                    Color.blue(top), Color.blue(bottom),
                    Color.blue(left), Color.blue(right), scale)
                
                sharpened[index] = Color.rgb(r, g, b)
            }
        }
        
        // Copy edges
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

    private fun sharpenChannel(center: Int, top: Int, bottom: Int, left: Int, right: Int, scale: Float): Int {
        val sum = (top + bottom + left + right) / 4
        val sharpened = center + ((center - sum) * scale).toInt()
        return sharpened.coerceIn(0, 255)
    }

    // FUNCTION 5: SAVE AS PDF
    fun saveAsPdf(bitmap: Bitmap, name: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            withContext(Dispatchers.IO) {
                try {
                    val vaultDir = File(context.filesDir, "vault")
                    if (!vaultDir.exists()) vaultDir.mkdirs()

                    val pdfDocument = PdfDocument()
                    
                    // Compress bitmap
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val compressedData = outputStream.toByteArray()
                    val compressedBitmap = android.graphics.BitmapFactory.decodeByteArray(
                        compressedData, 0, compressedData.size
                    )

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        compressedBitmap.width,
                        compressedBitmap.height,
                        1
                    ).create()
                    
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)

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
