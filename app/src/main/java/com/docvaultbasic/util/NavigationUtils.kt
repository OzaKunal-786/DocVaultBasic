package com.docvaultbasic.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.navigation.NavController
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.ui.navigation.Screen
import java.io.File

object NavigationUtils {
    fun openDocument(context: Context, navController: NavController, document: DocumentEntity) {
        val path = resolvePath(document)
        
        if (!pathExists(context, path)) {
            Toast.makeText(context, "File not accessible: ${document.fileName}", Toast.LENGTH_LONG).show()
            return
        }

        if (path.lowercase().contains(".pdf")) {
            navController.navigate(Screen.PdfViewer.createRoute(document.id))
        } else {
            // Ensure path is correctly formatted as a URI for the editor
            val finalUri = formatAsUri(path)
            navController.navigate(Screen.ImageEditor.createRoute(finalUri))
        }
    }

    fun editDocument(context: Context, navController: NavController, document: DocumentEntity) {
        val path = resolvePath(document)
        
        if (path.lowercase().contains(".pdf")) {
            Toast.makeText(context, "PDF files cannot be edited", Toast.LENGTH_SHORT).show()
        } else {
            if (!pathExists(context, path)) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
                return
            }
            val finalUri = formatAsUri(path)
            navController.navigate(Screen.ImageEditor.createRoute(finalUri))
        }
    }

    fun resolvePath(document: DocumentEntity): String {
        return if (document.storedPath.isNotBlank()) document.storedPath else document.originalPath
    }

    private fun formatAsUri(path: String): String {
        val uri = Uri.parse(path)
        return if (uri.scheme != null) path else Uri.fromFile(File(path)).toString()
    }

    fun pathExists(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        val uri = try { Uri.parse(path) } catch (e: Exception) { null } ?: return false

        return try {
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { it.close() }
                true
            } else {
                val filePath = if (uri.scheme == "file") uri.path else path
                File(filePath ?: "").exists()
            }
        } catch (e: Exception) {
            false
        }
    }
}
