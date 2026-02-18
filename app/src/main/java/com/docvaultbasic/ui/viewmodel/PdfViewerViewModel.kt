package com.docvaultbasic.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    fun getDocument(id: Int): Flow<DocumentEntity?> = flow {
        emit(documentRepository.getDocumentById(id))
    }
}
