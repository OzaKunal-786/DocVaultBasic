package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.data.repository.DocumentRepository
import com.docvaultbasic.util.DocumentScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val totalDocuments: Int = 0,
    val totalSize: Long = 0,
    val recentDocuments: List<DocumentEntity> = emptyList(),
    val allDocuments: List<DocumentEntity> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val documentScanner: DocumentScanner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                documentRepository.allDocuments,
                documentRepository.recentDocuments,
                documentRepository.totalSize,
                documentRepository.documentCount
            ) { allDocs, recentDocs, totalSize, docCount ->
                HomeUiState(
                    isLoading = false,
                    totalDocuments = docCount,
                    totalSize = totalSize ?: 0L,
                    recentDocuments = recentDocs,
                    allDocuments = allDocs
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            documentScanner.scanDocuments()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            documentRepository.deleteDocument(document)
        }
    }

    fun renameDocument(document: DocumentEntity, newName: String) {
        viewModelScope.launch {
            documentRepository.updateDocument(document.copy(fileName = newName))
        }
    }

    fun shareDocument(document: DocumentEntity) {
        try {
            // Standardize path: remove "file://" prefix if present
            val rawPath = document.originalPath.removePrefix("file://")
            val file = File(rawPath)
            
            if (!file.exists()) return

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Share PDF Document").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
