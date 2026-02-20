package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.viewmodel.PdfViewerViewModel
import com.docvaultbasic.util.EncryptionManager
import com.docvaultbasic.util.NavigationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentId: Int,
    navController: NavController,
    viewModel: PdfViewerViewModel = hiltViewModel(),
    encryptionManager: EncryptionManager = remember { EncryptionManager() }
) {
    val document by viewModel.getDocument(documentId).collectAsState(initial = null)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = document?.fileName ?: "PDF Viewer",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color.DarkGray)
        ) {
            document?.let { doc ->
                val path = NavigationUtils.resolvePath(doc)
                
                // Use produceState to handle decryption into a temp file
                val pdfResource = produceState<File?>(initialValue = null, path) {
                    withContext(Dispatchers.IO) {
                        try {
                            val uri = Uri.parse(path)
                            if (doc.isEncrypted) {
                                // Decrypt to a temporary file
                                val tempFile = File(context.cacheDir, "temp_view_${doc.id}.pdf")
                                
                                val inputStream = if (uri.scheme == "content") {
                                    context.contentResolver.openInputStream(uri)
                                } else {
                                    val file = if (uri.scheme == "file") File(uri.path ?: "") else File(path)
                                    file.inputStream()
                                }

                                inputStream?.use { input ->
                                    encryptionManager.decryptStream(input).use { decrypted ->
                                        tempFile.outputStream().use { output ->
                                            decrypted.copyTo(output)
                                        }
                                    }
                                }
                                value = tempFile
                            } else {
                                // If not encrypted, we still need a local file for PdfRenderer
                                if (uri.scheme == "content") {
                                    val tempFile = File(context.cacheDir, "temp_view_plain_${doc.id}.pdf")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        tempFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    value = tempFile
                                } else {
                                    val file = if (uri.scheme == "file") File(uri.path ?: "") else File(path)
                                    value = if (file.exists()) file else null
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            value = null
                        }
                    }
                }

                val pfdState = remember(pdfResource.value) {
                    try {
                        pdfResource.value?.let { 
                            ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                if (pfdState != null) {
                    val renderer = remember(pfdState) {
                        try { PdfRenderer(pfdState) } catch (e: Exception) { null }
                    }

                    if (renderer != null) {
                        DisposableEffect(renderer) {
                            onDispose {
                                try {
                                    renderer.close()
                                    pfdState.close()
                                    // Cleanup temp files
                                    File(context.cacheDir, "temp_view_${doc.id}.pdf").delete()
                                    File(context.cacheDir, "temp_view_plain_${doc.id}.pdf").delete()
                                } catch (e: Exception) { }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(renderer.pageCount) { index ->
                                PdfPageImage(renderer, index)
                            }
                        }
                    } else {
                        try { pfdState.close() } catch (e: Exception) {}
                        ErrorMessage("Could not initialize PDF renderer. The file might be corrupted.")
                    }
                } else if (pdfResource.value == null && path.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    ErrorMessage("File not found or inaccessible: $path")
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PdfPageImage(renderer: PdfRenderer, pageIndex: Int) {
    val density = LocalDensity.current.density
    val bitmapState = produceState<Bitmap?>(initialValue = null, renderer, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                renderer.openPage(pageIndex).use { page ->
                    val targetWidth = (page.width * density).toInt().coerceAtMost(2000)
                    val targetHeight = (page.height * (targetWidth.toFloat() / (page.width * density)) * density).toInt()
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    value = bitmap
                }
            } catch (e: Exception) {
                value = null
            }
        }
    }

    bitmapState.value?.let { bitmap ->
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
