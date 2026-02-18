package com.docvaultbasic.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.ui.navigation.Screen
import com.docvaultbasic.ui.viewmodel.SettingsViewModel
import com.docvaultbasic.util.checkAllFilesAccess
import com.docvaultbasic.util.requestAllFilesAccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.createBackup(it) }
        }
    )

    LaunchedEffect(uiState.backupSuccess, uiState.errorMessage) {
        if (uiState.backupSuccess == true) {
            scope.launch {
                snackbarHostState.showSnackbar("Backup created successfully!")
                viewModel.resetStatus()
            }
        } else if (uiState.errorMessage != null) {
            scope.launch {
                snackbarHostState.showSnackbar("Backup failed: ${uiState.errorMessage}")
                viewModel.resetStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Security Section
            Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            SettingSwitchItem(title = "Enable Biometric Authentication", icon = Icons.Default.Https, checked = uiState.isBiometricEnabled) {
                viewModel.setBiometricEnabled(it)
            }
            HorizontalDivider()
            SettingClickableItem(title = "Change PIN", icon = Icons.Default.Https) {
                navController.navigate(Screen.PinSetup.route)
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Permissions Section
            Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            SettingClickableItem(
                title = "Vault Deep Scan Access", 
                icon = Icons.Default.Security,
                onClick = { requestAllFilesAccess(context) }
            )
            val hasDeepAccess = checkAllFilesAccess()
            Text(
                if (hasDeepAccess) "Status: Granted" else "Status: Not Granted",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasDeepAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 56.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Storage Section
            Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            SettingClickableItem(title = "Manage Folders", icon = Icons.Default.Folder) {
                navController.navigate(Screen.FolderSelection.route)
            }
            HorizontalDivider()
            SettingClickableItem(title = "Clear Cache (${uiState.cacheSize / 1024} KB)", icon = Icons.Default.DeleteSweep) {
                viewModel.clearCache()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Backup & Restore Section
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            SettingClickableItem(title = "Backup Vault", icon = Icons.Default.Backup) {
                backupLauncher.launch("DocVaultBackup.db.xz")
            }
            HorizontalDivider()
            SettingClickableItem(title = "Restore Vault", icon = Icons.Default.Restore) {
                // TODO: Restore Logic
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // About section
            Text(
                text = "Version: ${getAppVersion(context)}", 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun SettingSwitchItem(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingClickableItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}
