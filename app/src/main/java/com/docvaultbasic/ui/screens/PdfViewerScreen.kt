package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.viewmodel.PdfViewerViewModel
import com.docvaultbasic.util.NavigationUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    documentId: Int,
    navController: NavController,
    viewModel: PdfViewerViewModel = hiltViewModel()
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
                val uri = Uri.parse(path)
                
                val pfd: ParcelFileDescriptor? = try {
                    if (uri.scheme == "content") {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    } else {
                        val file = if (uri.scheme == "file") File(uri.path ?: "") else File(path)
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (pfd != null) {
                    val rendererResource = remember(pfd) {
                        try {
                            PdfRenderer(pfd)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (rendererResource != null) {
                        DisposableEffect(rendererResource) {
                            onDispose {
                                try {
                                    rendererResource.close()
                                    pfd.close()
                                } catch (e: Exception) { }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(rendererResource.pageCount) { index ->
                                val bitmap = remember(rendererResource, index) {
                                    try {
                                        rendererResource.openPage(index).use { page ->
                                            val b = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                            page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            b
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                
                                if (bitmap != null) {
                                    Card(
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Page ${index + 1}",
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        pfd.close()
                        ErrorMessage("Could not initialize PDF renderer. The file might be corrupted.")
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
