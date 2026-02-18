package com.docvaultbasic.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvaultbasic.security.PinManager
import com.docvaultbasic.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isBiometricEnabled: Boolean = false,
    val cacheSize: Long = 0,
    val backupSuccess: Boolean? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pinManager: PinManager,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val cache = context.cacheDir
            val size = cache.walkTopDown().sumOf { it.length() }
            _uiState.update {
                it.copy(
                    isBiometricEnabled = pinManager.isBiometricEnabled(),
                    cacheSize = size
                )
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        pinManager.setBiometricEnabled(enabled)
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun clearCache() {
        viewModelScope.launch {
            context.cacheDir.deleteRecursively()
            loadSettings()
        }
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                backupManager.createBackup(uri)
                _uiState.update { it.copy(backupSuccess = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(backupSuccess = false, errorMessage = e.message) }
            }
        }
    }

    fun resetStatus() {
        _uiState.update { it.copy(backupSuccess = null, errorMessage = null) }
    }
    
    fun restoreVault(uri: Uri) {
        // TODO: Implement restore logic in BackupManager
    }
}
