package com.docvaultbasic.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.viewmodel.PdfViewerViewModel

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
                title = { Text(document?.fileName ?: "PDF Viewer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            document?.let { doc ->
                val uri = Uri.parse(doc.originalPath)
                val pfd: ParcelFileDescriptor? = try {
                    context.contentResolver.openFileDescriptor(uri, "r")
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                if (pfd != null) {
                    val renderer = remember(pfd) { PdfRenderer(pfd) }

                    DisposableEffect(renderer) {
                        onDispose {
                            renderer.close()
                            pfd.close()
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(renderer.pageCount) { index ->
                            val bitmap = remember(index) {
                                renderer.openPage(index).use { page ->
                                    val b = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                    page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    b
                                }
                            }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page $index",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Could not open PDF file. Make sure you have granted permission.")
                    }
                }
            }
        }
    }
}
