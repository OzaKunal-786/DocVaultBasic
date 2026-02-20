package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvaultbasic.data.database.FolderEntity
import com.docvaultbasic.data.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val allFolders: Flow<List<FolderEntity>> = folderRepository.allFolders

    init {
        viewModelScope.launch {
            val existingFolders = folderRepository.allFolders.first()
            if (existingFolders.isEmpty()) {
                addDefaultFolders()
            }
        }
    }

    private suspend fun addDefaultFolders() {
        val paths = mutableSetOf<String>()
        val defaultFolders = mutableListOf<FolderEntity>()

        fun addIfUnique(file: File, name: String) {
            val path = file.absolutePath
            // Ensure path is unique and normalize it
            val normalizedPath = path.trimEnd('/')
            if (file.exists() && paths.add(normalizedPath)) {
                defaultFolders.add(FolderEntity(folderPath = normalizedPath, folderName = name))
            }
        }

        // Downloads
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        addIfUnique(downloads, "Downloads")

        // WhatsApp Documents (Common path)
        val whatsappDocs = File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents")
        addIfUnique(whatsappDocs, "WhatsApp Documents")

        // Documents
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        addIfUnique(docs, "Documents")

        defaultFolders.forEach { folderRepository.insertFolder(it) }
    }

    fun addFolder(path: String, name: String) {
        viewModelScope.launch {
            val existing = folderRepository.allFolders.first()
            val normalizedPath = path.trimEnd('/')
            if (existing.none { it.folderPath.trimEnd('/') == normalizedPath }) {
                folderRepository.insertFolder(FolderEntity(folderPath = normalizedPath, folderName = name))
            }
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            folderRepository.updateFolder(folder)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folder)
        }
    }
}
