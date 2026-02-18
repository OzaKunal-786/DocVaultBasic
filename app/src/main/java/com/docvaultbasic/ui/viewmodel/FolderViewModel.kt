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
        val defaultFolders = mutableListOf<FolderEntity>()

        // Downloads
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads.exists()) {
            defaultFolders.add(FolderEntity(folderPath = downloads.absolutePath, folderName = "Downloads"))
        }

        // WhatsApp Documents (Common path)
        val whatsappDocs = File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents")
        if (whatsappDocs.exists()) {
            defaultFolders.add(FolderEntity(folderPath = whatsappDocs.absolutePath, folderName = "WhatsApp Documents"))
        }

        // Documents
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (docs.exists()) {
            defaultFolders.add(FolderEntity(folderPath = docs.absolutePath, folderName = "Documents"))
        }

        defaultFolders.forEach { folderRepository.insertFolder(it) }
    }

    fun addFolder(path: String, name: String) {
        viewModelScope.launch {
            folderRepository.insertFolder(FolderEntity(folderPath = path, folderName = name))
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
