package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.navigation.Screen
import com.docvaultbasic.ui.viewmodel.ImageEditorViewModel
import com.docvaultbasic.util.FileHelper

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
    var rotation by remember { mutableStateOf(0f) }
    var showNameDialog by remember { mutableStateOf(false) }
    
    val originalName = remember(imageUri) { FileHelper.getFileName(context, uri) }
    val cleanName = originalName.substringBeforeLast(".")
    var documentName by remember { mutableStateOf(cleanName) }

    val processedBitmap = remember(brightness, contrast, rotation, originalBitmap) {
        viewModel.processBitmap(originalBitmap, brightness, contrast, rotation)
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showNameDialog = false },
            title = { Text("Save to Vault") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text("Finalizing PDF...", style = MaterialTheme.typography.bodySmall)
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
                title = { Text("Edit & Enhance") },
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (originalBitmap != null) {
                    Image(
                        bitmap = processedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Failed to load image preview.", color = MaterialTheme.colorScheme.error)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Brightness
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Contrast
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Contrast, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Slider(
                        value = contrast,
                        onValueChange = { contrast = it },
                        valueRange = 0.5f..2f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                    }
                    
                    Button(
                        onClick = { showNameDialog = true },
                        modifier = Modifier.height(48.dp),
                        enabled = originalBitmap != null
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Finish & Save")
                    }
                }
            }
        }
    }
}
