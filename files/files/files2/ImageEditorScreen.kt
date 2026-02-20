package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val uri = Uri.parse(imageUri)
    
    val originalBitmap = remember { 
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
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
    
    val originalName = remember(imageUri) { FileHelper.getFileName(context, uri) }
    val cleanName = originalName.substringBeforeLast(".")
    var documentName by remember { mutableStateOf(cleanName) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Handle back press in crop mode
    BackHandler(enabled = activeTool == EditTool.CROP) {
        activeTool = EditTool.NONE
        cropCorners = emptyList()
    }

    LaunchedEffect(activeTool) {
        if (activeTool == EditTool.CROP && cropCorners.isEmpty() && currentBitmap != null) {
            cropCorners = viewModel.detectDocumentEdges(currentBitmap!!)
        }
    }

    val processedBitmap = remember(
        currentBitmap, brightness, contrast, saturation, sharpness, rotation, selectedFilter
    ) {
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
                if (!isSaving) {
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
                title = { 
                    Text(
                        if (activeTool == EditTool.CROP) "Crop Document" else "Edit",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (activeTool == EditTool.CROP) {
                            activeTool = EditTool.NONE
                            cropCorners = emptyList()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            if (activeTool == EditTool.CROP) Icons.Default.Close 
                            else Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    if (activeTool != EditTool.CROP) {
                        IconButton(onClick = { showNameDialog = true }) {
                            Icon(Icons.Default.Check, "Save", Modifier.size(22.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // IMAGE PREVIEW
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { imageSize = it },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = if (activeTool == EditTool.CROP) currentBitmap!!.asImageBitmap() 
                                    else processedBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        if (activeTool == EditTool.CROP && cropCorners.isNotEmpty()) {
                            CropOverlay(
                                corners = cropCorners,
                                imageSize = imageSize,
                                currentBitmap = currentBitmap!!,
                                onCornersChanged = { cropCorners = it }
                            )
                        }
                    }
                } else {
                    Text("Failed to load image", color = Color.White)
                }
            }

            // CONTROLS - SCROLLABLE!
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()) // SCROLLABLE!
                ) {
                    if (activeTool == EditTool.CROP) {
                        // CROP MODE CONTROLS
                        CropControls(
                            onAutoDetect = {
                                currentBitmap?.let {
                                    cropCorners = viewModel.detectDocumentEdges(it)
                                }
                            },
                            onApply = {
                                if (cropCorners.size == 4) {
                                    currentBitmap = viewModel.applyPerspectiveCorrection(
                                        currentBitmap!!,
                                        cropCorners
                                    )
                                    brightness = 0f
                                    contrast = 1f
                                    saturation = 1f
                                    sharpness = 0f
                                    rotation = 0f
                                    activeTool = EditTool.NONE
                                    cropCorners = emptyList()
                                }
                            },
                            onCancel = {
                                activeTool = EditTool.NONE
                                cropCorners = emptyList()
                            },
                            enabled = cropCorners.size == 4
                        )
                    } else {
                        // NORMAL EDIT MODE
                        when (activeTool) {
                            EditTool.BRIGHTNESS -> {
                                AdjustmentSlider(
                                    icon = Icons.Default.LightMode,
                                    label = "Brightness",
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -100f..100f,
                                    onClose = { activeTool = EditTool.NONE }
                                )
                            }
                            EditTool.CONTRAST -> {
                                AdjustmentSlider(
                                    icon = Icons.Default.Contrast,
                                    label = "Contrast",
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0.5f..2f,
                                    onClose = { activeTool = EditTool.NONE }
                                )
                            }
                            EditTool.SATURATION -> {
                                AdjustmentSlider(
                                    icon = Icons.Default.Palette,
                                    label = "Saturation",
                                    value = saturation,
                                    onValueChange = { saturation = it },
                                    valueRange = 0f..2f,
                                    onClose = { activeTool = EditTool.NONE }
                                )
                            }
                            EditTool.SHARPNESS -> {
                                AdjustmentSlider(
                                    icon = Icons.Default.AutoFixHigh,
                                    label = "Sharpness",
                                    value = sharpness,
                                    onValueChange = { sharpness = it },
                                    valueRange = 0f..10f,
                                    onClose = { activeTool = EditTool.NONE }
                                )
                            }
                            EditTool.FILTERS -> {
                                FilterSelector(
                                    selectedFilter = selectedFilter,
                                    onFilterSelected = { selectedFilter = it },
                                    onClose = { activeTool = EditTool.NONE }
                                )
                            }
                            else -> {}
                        }

                        // BOTTOM TOOLBAR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolButton(
                                icon = Icons.Default.Image,
                                label = "Filter",
                                isActive = activeTool == EditTool.FILTERS,
                                onClick = { 
                                    activeTool = if (activeTool == EditTool.FILTERS) EditTool.NONE 
                                                 else EditTool.FILTERS 
                                }
                            )
                            
                            ToolButton(
                                icon = Icons.Default.LightMode,
                                label = "Light",
                                isActive = activeTool == EditTool.BRIGHTNESS,
                                onClick = { 
                                    activeTool = if (activeTool == EditTool.BRIGHTNESS) EditTool.NONE 
                                                 else EditTool.BRIGHTNESS 
                                }
                            )
                            
                            ToolButton(
                                icon = Icons.Default.Contrast,
                                label = "Contrast",
                                isActive = activeTool == EditTool.CONTRAST,
                                onClick = { 
                                    activeTool = if (activeTool == EditTool.CONTRAST) EditTool.NONE 
                                                 else EditTool.CONTRAST 
                                }
                            )
                            
                            ToolButton(
                                icon = Icons.Default.Palette,
                                label = "Color",
                                isActive = activeTool == EditTool.SATURATION,
                                onClick = { 
                                    activeTool = if (activeTool == EditTool.SATURATION) EditTool.NONE 
                                                 else EditTool.SATURATION 
                                }
                            )
                            
                            ToolButton(
                                icon = Icons.Default.AutoFixHigh,
                                label = "Sharp",
                                isActive = activeTool == EditTool.SHARPNESS,
                                onClick = { 
                                    activeTool = if (activeTool == EditTool.SHARPNESS) EditTool.NONE 
                                                 else EditTool.SHARPNESS 
                                }
                            )
                            
                            ToolButton(
                                icon = Icons.Default.RotateRight,
                                label = "Rotate",
                                onClick = { rotation = (rotation + 90f) % 360f }
                            )
                            
                            ToolButton(
                                icon = Icons.Default.Crop,
                                label = "Crop",
                                isActive = activeTool == EditTool.CROP,
                                onClick = { activeTool = EditTool.CROP }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primaryContainer 
                    else Color.Transparent
                ),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                              else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isActive) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun AdjustmentSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Check, "Done", Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun FilterSelector(
    selectedFilter: FilterMode,
    onFilterSelected: (FilterMode) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Check, "Done", Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterMode.values().forEach { mode ->
                    FilterChipCompact(
                        label = when (mode) {
                            FilterMode.ORIGINAL -> "Original"
                            FilterMode.MAGIC_COLOR -> "Magic"
                            FilterMode.GRAYSCALE -> "Gray"
                            FilterMode.BLACK_WHITE -> "B&W"
                        },
                        isSelected = selectedFilter == mode,
                        onClick = { onFilterSelected(mode) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipCompact(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
               else MaterialTheme.colorScheme.surface,
        modifier = Modifier.height(32.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CropControls(
    onAutoDetect: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Drag corners to adjust selection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onAutoDetect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Auto Detect", fontSize = 13.sp)
            }
            
            Spacer(Modifier.width(8.dp))
            
            Button(
                onClick = onApply,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply", fontSize = 13.sp)
            }
            
            Spacer(Modifier.width(8.dp))
            
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", fontSize = 13.sp)
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

    val scaleX = imageSize.width.toFloat() / currentBitmap.width
    val scaleY = imageSize.height.toFloat() / currentBitmap.height
    val scale = minOf(scaleX, scaleY)
    
    val displayWidth = currentBitmap.width * scale
    val displayHeight = currentBitmap.height * scale
    val offsetX = (imageSize.width - displayWidth) / 2
    val offsetY = (imageSize.height - displayHeight) / 2
    
    fun bitmapToScreen(point: PointF): Offset {
        return Offset(point.x * scale + offsetX, point.y * scale + offsetY)
    }
    
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
                        val screenCorners = corners.map { bitmapToScreen(it) }
                        val touchRadius = 80f // BIGGER TOUCH AREA!
                        draggedCornerIndex = screenCorners.indexOfFirst { corner ->
                            (offset - corner).getDistance() < touchRadius
                        }.takeIf { it >= 0 }
                    },
                    onDrag = { change, _ ->
                        change.consume() // Prevent back swipe!
                        draggedCornerIndex?.let { index ->
                            val newCorners = corners.toMutableList()
                            newCorners[index] = screenToBitmap(change.position)
                            onCornersChanged(newCorners)
                        }
                    },
                    onDragEnd = { draggedCornerIndex = null }
                )
            }
    ) {
        val screenCorners = corners.map { bitmapToScreen(it) }
        
        // Draw lines
        for (i in 0 until 4) {
            drawLine(
                color = Color(0xFF2196F3),
                start = screenCorners[i],
                end = screenCorners[(i + 1) % 4],
                strokeWidth = 4f
            )
        }
        
        // Draw BIGGER corner handles
        screenCorners.forEach { corner ->
            drawCircle(
                color = Color.White,
                radius = 20f, // BIGGER!
                center = corner
            )
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 20f,
                center = corner,
                style = Stroke(width = 4f)
            )
        }
    }
}
