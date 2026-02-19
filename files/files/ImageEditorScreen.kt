package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.navigation.Screen
import com.docvaultbasic.ui.viewmodel.FilterMode
import com.docvaultbasic.ui.viewmodel.ImageEditorViewModel
import com.docvaultbasic.util.FileHelper
import android.graphics.PointF

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: String,
    navController: NavController,
    viewModel: ImageEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsState()
    val uri = Uri.parse(imageUri)
    
    // Load original bitmap
    val originalBitmap = remember { 
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    // Editing states
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var saturation by remember { mutableStateOf(1f) }  // NEW
    var sharpness by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(FilterMode.ORIGINAL) }
    
    // Crop mode states
    var showCropMode by remember { mutableStateOf(false) }
    var cropCorners by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var currentBitmap by remember { mutableStateOf(originalBitmap) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    
    // File name
    val originalName = remember(imageUri) { FileHelper.getFileName(context, uri) }
    val cleanName = originalName.substringBeforeLast(".")
    var documentName by remember { mutableStateOf(cleanName) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Auto-detect edges when entering crop mode
    LaunchedEffect(showCropMode) {
        if (showCropMode && cropCorners.isEmpty() && currentBitmap != null) {
            cropCorners = viewModel.detectDocumentEdges(currentBitmap!!)
        }
    }

    // Process bitmap with filters and adjustments
    val processedBitmap = remember(
        currentBitmap, brightness, contrast, saturation, sharpness, rotation, selectedFilter
    ) {
        val adjusted = viewModel.processBitmap(
            currentBitmap, brightness, contrast, saturation, sharpness, rotation
        )
        viewModel.applyFilter(adjusted, selectedFilter)
    }

    // Save dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showNameDialog = false },
            title = { Text("Save to Vault") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text("Creating PDF...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Enter a name for your document:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = documentName,
                            onValueChange = { documentName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            suffix = { Text(".pdf") }
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSaving) {
                    Button(onClick = {
                        viewModel.saveAsPdf(processedBitmap, documentName) {
                            showNameDialog = false
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }) { Text("Save PDF") }
                }
            },
            dismissButton = {
                if (!isSaving) {
                    Button(onClick = { showNameDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showCropMode) "Crop Document" else "Edit & Enhance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Image preview with crop overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onSizeChanged { imageSize = it },
                contentAlignment = Alignment.Center
            ) {
                if (originalBitmap != null) {
                    // Show image
                    Image(
                        bitmap = if (showCropMode) currentBitmap!!.asImageBitmap() 
                                else processedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Show crop overlay if in crop mode
                    if (showCropMode && cropCorners.isNotEmpty()) {
                        CropOverlay(
                            corners = cropCorners,
                            imageSize = imageSize,
                            currentBitmap = currentBitmap!!,
                            onCornersChanged = { cropCorners = it }
                        )
                    }
                } else {
                    Text("Failed to load image", color = MaterialTheme.colorScheme.error)
                }
            }

            // Controls area
            if (!showCropMode) {
                // NORMAL EDIT MODE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Filter chips
                    Text("Filter Presets", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterMode.values().forEach { mode ->
                            FilterChip(
                                selected = selectedFilter == mode,
                                onClick = { selectedFilter = mode },
                                label = { 
                                    Text(
                                        when (mode) {
                                            FilterMode.ORIGINAL -> "Original"
                                            FilterMode.MAGIC_COLOR -> "Magic"
                                            FilterMode.GRAYSCALE -> "Gray"
                                            FilterMode.BLACK_WHITE -> "B&W"
                                        }
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manual adjustments
                    Text("Adjustments", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Brightness
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LightMode, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Brightness", Modifier.weight(0.3f), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = -100f..100f,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Contrast
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Contrast, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Contrast", Modifier.weight(0.3f), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = contrast,
                            onValueChange = { contrast = it },
                            valueRange = 0.5f..2f,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Saturation (NEW)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Saturation", Modifier.weight(0.3f), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = saturation,
                            onValueChange = { saturation = it },
                            valueRange = 0f..2f,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Sharpness
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sharpness", Modifier.weight(0.3f), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = sharpness,
                            onValueChange = { sharpness = it },
                            valueRange = 0f..10f,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Rotate button
                        Button(
                            onClick = { rotation = (rotation + 90f) % 360f },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.RotateRight, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Rotate")
                        }

                        // Crop button
                        Button(
                            onClick = { showCropMode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Crop, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Crop")
                        }

                        // Finish button
                        Button(
                            onClick = { showNameDialog = true },
                            enabled = originalBitmap != null
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            } else {
                // CROP MODE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Drag the corners to select document area",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Auto-detect button
                        Button(
                            onClick = {
                                currentBitmap?.let {
                                    cropCorners = viewModel.detectDocumentEdges(it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text("Auto Detect")
                        }

                        // Apply crop button
                        Button(
                            onClick = {
                                if (cropCorners.size == 4) {
                                    currentBitmap = viewModel.applyPerspectiveCorrection(
                                        currentBitmap!!,
                                        cropCorners
                                    )
                                    // Reset adjustments after crop
                                    brightness = 0f
                                    contrast = 1f
                                    saturation = 1f
                                    sharpness = 0f
                                    rotation = 0f
                                    selectedFilter = FilterMode.ORIGINAL
                                    showCropMode = false
                                    cropCorners = emptyList()
                                }
                            },
                            enabled = cropCorners.size == 4
                        ) {
                            Text("Apply Crop")
                        }

                        // Cancel button
                        Button(
                            onClick = {
                                showCropMode = false
                                cropCorners = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CropOverlay(
    corners: List<PointF>,
    imageSize: IntSize,
    currentBitmap: Bitmap,
    onCornersChanged: (List<PointF>) -> Unit
) {
    if (corners.size != 4) return

    val density = LocalDensity.current
    
    // Calculate scale factor between actual bitmap and displayed image
    val scaleX = imageSize.width.toFloat() / currentBitmap.width
    val scaleY = imageSize.height.toFloat() / currentBitmap.height
    val scale = minOf(scaleX, scaleY)
    
    // Calculate offset to center image
    val displayWidth = currentBitmap.width * scale
    val displayHeight = currentBitmap.height * scale
    val offsetX = (imageSize.width - displayWidth) / 2
    val offsetY = (imageSize.height - displayHeight) / 2
    
    // Convert bitmap coordinates to screen coordinates
    fun bitmapToScreen(point: PointF): Offset {
        return Offset(
            point.x * scale + offsetX,
            point.y * scale + offsetY
        )
    }
    
    // Convert screen coordinates to bitmap coordinates
    fun screenToBitmap(offset: Offset): PointF {
        return PointF(
            ((offset.x - offsetX) / scale).coerceIn(0f, currentBitmap.width.toFloat()),
            ((offset.y - offsetY) / scale).coerceIn(0f, currentBitmap.height.toFloat())
        )
    }
    
    var draggedCornerIndex by remember { mutableStateOf<Int?>(null) }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find which corner is being dragged
                        val screenCorners = corners.map { bitmapToScreen(it) }
                        val touchRadius = 50f
                        draggedCornerIndex = screenCorners.indexOfFirst { corner ->
                            (offset - corner).getDistance() < touchRadius
                        }.takeIf { it >= 0 }
                    },
                    onDrag = { change, _ ->
                        draggedCornerIndex?.let { index ->
                            val newCorners = corners.toMutableList()
                            newCorners[index] = screenToBitmap(change.position)
                            onCornersChanged(newCorners)
                        }
                    },
                    onDragEnd = {
                        draggedCornerIndex = null
                    }
                )
            }
    ) {
        val screenCorners = corners.map { bitmapToScreen(it) }
        
        // Draw lines between corners
        for (i in 0 until 4) {
            val start = screenCorners[i]
            val end = screenCorners[(i + 1) % 4]
            drawLine(
                color = Color.Blue,
                start = start,
                end = end,
                strokeWidth = 4f
            )
        }
        
        // Draw corner handles
        screenCorners.forEach { corner ->
            drawCircle(
                color = Color.Blue,
                radius = 20f,
                center = corner,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = Color.White,
                radius = 16f,
                center = corner
            )
        }
    }
}
