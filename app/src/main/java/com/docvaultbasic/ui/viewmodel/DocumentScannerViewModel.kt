package com.docvaultbasic.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class ScannerState {
    object Idle : ScannerState()
    object Processing : ScannerState()
    data class Success(val imagePath: String) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

@HiltViewModel
class DocumentScannerViewModel @Inject constructor() : ViewModel() {

    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val scannerState: StateFlow<ScannerState> = _scannerState

    fun onImageCaptured(path: String) {
        _scannerState.value = ScannerState.Success(path)
    }

    fun onError(error: String) {
        _scannerState.value = ScannerState.Error(error)
    }

    fun resetState() {
        _scannerState.value = ScannerState.Idle
    }
}
