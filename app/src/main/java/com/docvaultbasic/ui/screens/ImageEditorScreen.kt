package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.navigation.Screen
import com.docvaultbasic.ui.viewmodel.FilterMode
import com.docvaultbasic.ui.viewmodel.ImageEditorViewModel
import com.docvaultbasic.util.FileHelper

enum class EditTool {
    NONE,
    FILTERS,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    SHARPNESS,
    ROTATE,
    CROP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: String,
    navController: NavController,
    viewModel: ImageEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsState()
    
    val decodedUri = remember(imageUri) { 
        try { java.net.URLDecoder.decode(imageUri, "UTF-8") } catch (e: Exception) { imageUri }
    }
    val uri = remember(decodedUri) {
        try { Uri.parse(decodedUri) } catch (e: Exception) { null }
    }
    
    val originalBitmap = remember(uri) { 
        try {
            if (uri != null) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                }
                context.contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else null
        } catch (e: Exception) { null }
    }
    
    var brightness by remember { mutableStateOf(0f) }
    var contrast by remember { mutableStateOf(1f) }
    var saturation by remember { mutableStateOf(1f) }
    var sharpness by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }
    var selectedFilter by remember { mutableStateOf(FilterMode.ORIGINAL) }
    var activeTool by remember { mutableStateOf(EditTool.NONE) }
    
    var cropCorners by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var currentBitmap by remember { mutableStateOf(originalBitmap) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val originalName = remember(decodedUri) { 
        uri?.let { FileHelper.getFileName(context, it) } ?: "Document"
    }
    val cleanName = originalName.substringBeforeLast(".")
    var documentName by remember { mutableStateOf(cleanName) }
    var showNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(originalBitmap) {
        if (originalBitmap == null && uri != null) {
            Toast.makeText(context, "Error: Could not load image", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    BackHandler(enabled = activeTool == EditTool.CROP) {
        activeTool = EditTool.NONE
        cropCorners = emptyList()
    }

    LaunchedEffect(activeTool) {
        if (activeTool == EditTool.CROP && cropCorners.isEmpty() && currentBitmap != null) {
            cropCorners = viewModel.detectDocumentEdges(currentBitmap!!)
        }
        if (activeTool == EditTool.CROP) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    val processedBitmap = remember(
        currentBitmap, brightness, contrast, saturation, sharpness, rotation, selectedFilter
    ) {
        if (currentBitmap == null) return@remember null
        val adjusted = viewModel.processBitmap(
            currentBitmap, brightness, contrast, saturation, sharpness, rotation
        )
        viewModel.applyFilter(adjusted, selectedFilter)
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showNameDialog = false },
            title = { Text("Save Document") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text("Creating PDF...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        OutlinedTextField(
                            value = documentName,
                            onValueChange = { documentName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Document name") },
                            suffix = { Text(".pdf") }
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSaving && processedBitmap != null) {
                    Button(onClick = {
                        viewModel.saveAsPdf(processedBitmap, documentName) {
                            showNameDialog = false
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }) { Text("Save") }
                }
            },
            dismissButton = {
                if (!isSaving) {
                    Button(
                        onClick = { showNameDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeTool == EditTool.CROP) "Crop" else "Edit") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (activeTool == EditTool.CROP) {
                            activeTool = EditTool.NONE
                            cropCorners = emptyList()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(if (activeTool == EditTool.CROP) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (activeTool != EditTool.CROP && processedBitmap != null) {
                        IconButton(onClick = { showNameDialog = true }) {
                            Icon(Icons.Default.Check, "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        if (activeTool != EditTool.CROP) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset += pan
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (activeTool == EditTool.CROP) 48.dp else 0.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .onSizeChanged { if (it.width > 0 && it.height > 0) imageSize = it },
                        contentAlignment = Alignment.Center
                    ) {
                        val displayBitmap = if (activeTool == EditTool.CROP) currentBitmap else processedBitmap
                        
                        if (displayBitmap != null) {
                            Image(
                                bitmap = displayBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        if (activeTool == EditTool.CROP && cropCorners.size == 4 && imageSize.width > 0) {
                            CropOverlay(
                                corners = cropCorners,
                                imageSize = imageSize,
                                currentBitmap = currentBitmap!!,
                                onCornersChanged = { cropCorners = it }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    if (activeTool == EditTool.CROP) {
                        CropControls(
                            onAutoDetect = {
                                currentBitmap?.let { cropCorners = viewModel.detectDocumentEdges(it) }
                            },
                            onApply = {
                                if (cropCorners.size == 4) {
                                    currentBitmap = viewModel.applyPerspectiveCorrection(currentBitmap!!, cropCorners)
                                    activeTool = EditTool.NONE
                                    cropCorners = emptyList()
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            onCancel = {
                                activeTool = EditTool.NONE
                                cropCorners = emptyList()
                            },
                            enabled = cropCorners.size == 4
                        )
                    } else {
                        when (activeTool) {
                            EditTool.BRIGHTNESS -> AdjustmentSlider(Icons.Default.LightMode, "Brightness", brightness, { brightness = it }, -100f..100f, { activeTool = EditTool.NONE })
                            EditTool.CONTRAST -> AdjustmentSlider(Icons.Default.Contrast, "Contrast", contrast, { contrast = it }, 0.5f..2f, { activeTool = EditTool.NONE })
                            EditTool.SATURATION -> AdjustmentSlider(Icons.Default.Palette, "Saturation", saturation, { saturation = it }, 0f..2f, { activeTool = EditTool.NONE })
                            EditTool.SHARPNESS -> AdjustmentSlider(Icons.Default.AutoFixHigh, "Sharpness", sharpness, { sharpness = it }, 0f..10f, { activeTool = EditTool.NONE })
                            EditTool.FILTERS -> FilterSelector(selectedFilter, { selectedFilter = it }, { activeTool = EditTool.NONE })
                            else -> {}
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolButton(Icons.Default.Image, "Filter", activeTool == EditTool.FILTERS) { activeTool = EditTool.FILTERS }
                            ToolButton(Icons.Default.LightMode, "Light", activeTool == EditTool.BRIGHTNESS) { activeTool = EditTool.BRIGHTNESS }
                            ToolButton(Icons.Default.Contrast, "Contrast", activeTool == EditTool.CONTRAST) { activeTool = EditTool.CONTRAST }
                            ToolButton(Icons.Default.Palette, "Color", activeTool == EditTool.SATURATION) { activeTool = EditTool.SATURATION }
                            ToolButton(Icons.Default.AutoFixHigh, "Sharp", activeTool == EditTool.SHARPNESS) { activeTool = EditTool.SHARPNESS }
                            ToolButton(Icons.Default.RotateRight, "Rotate") { rotation = (rotation + 90f) % 360f }
                            ToolButton(Icons.Default.Crop, "Crop", activeTool == EditTool.CROP) { activeTool = EditTool.CROP }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            colors = IconButtonDefaults.iconButtonColors(contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun AdjustmentSlider(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, onClose: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Check, "Done", Modifier.size(18.dp)) }
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
        }
    }
}

@Composable
fun FilterSelector(selectedFilter: FilterMode, onFilterSelected: (FilterMode) -> Unit, onClose: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Check, "Done", Modifier.size(18.dp)) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMode.values().forEach { mode ->
                    FilterChipCompact(mode.name.lowercase().replaceFirstChar { it.uppercase() }, selectedFilter == mode) { onFilterSelected(mode) }
                }
            }
        }
    }
}

@Composable
fun FilterChipCompact(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, modifier = Modifier.height(32.dp)) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
        }
    }
}

@Composable
fun CropControls(onAutoDetect: () -> Unit, onApply: () -> Unit, onCancel: () -> Unit, enabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = onAutoDetect, modifier = Modifier.weight(1f)) { Text("Auto") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onApply, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Apply") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Cancel") }
    }
}

@Composable
fun CropOverlay(corners: List<PointF>, imageSize: IntSize, currentBitmap: Bitmap, onCornersChanged: (List<PointF>) -> Unit) {
    if (corners.size != 4) return
    
    val scaleX = imageSize.width.toFloat() / currentBitmap.width
    val scaleY = imageSize.height.toFloat() / currentBitmap.height
    val scale = minOf(scaleX, scaleY)
    val offsetX = (imageSize.width - currentBitmap.width * scale) / 2
    val offsetY = (imageSize.height - currentBitmap.height * scale) / 2
    
    fun b2s(p: PointF) = Offset(p.x * scale + offsetX, p.y * scale + offsetY)
    fun s2b(o: Offset) = PointF(
        ((o.x - offsetX) / scale).coerceIn(0f, currentBitmap.width.toFloat()), 
        ((o.y - offsetY) / scale).coerceIn(0f, currentBitmap.height.toFloat())
    )
    
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    
    // Internal state to hold corners during drag to prevent jitter/reset
    val internalCorners = remember { mutableStateListOf<PointF>().apply { addAll(corners) } }
    
    // Sync with external state when not dragging
    LaunchedEffect(corners) {
        if (draggedIndex == null) {
            internalCorners.clear()
            internalCorners.addAll(corners)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { o ->
                draggedIndex = internalCorners.indices.indexOfFirst { (o - b2s(internalCorners[it])).getDistance() < 100f }.takeIf { it >= 0 }
            }, 
            onDrag = { change, _ ->
                draggedIndex?.let { i ->
                    val newPoint = s2b(change.position)
                    internalCorners[i] = newPoint
                    // Update parent immediately but using our local list to ensure stability
                    onCornersChanged(internalCorners.toList())
                }
            }, 
            onDragEnd = { draggedIndex = null }
        )
    }) {
        val s = internalCorners.map { b2s(it) }
        for (i in 0 until 4) drawLine(color = Color(0xFF2196F3), start = s[i], end = s[(i + 1) % 4], strokeWidth = 4f)
        s.forEach { corner ->
            drawCircle(color = Color.White, radius = 24f, center = corner)
            drawCircle(color = Color(0xFF2196F3), radius = 24f, center = corner, style = Stroke(width = 4f))
        }
    }
}
