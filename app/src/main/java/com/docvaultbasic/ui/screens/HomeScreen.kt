package com.docvaultbasic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.ui.navigation.Screen
import com.docvaultbasic.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DocVault", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            QuickStats(uiState.totalDocuments, uiState.totalSize)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.recentDocuments.isNotEmpty()) {
                RecentDocuments(
                    uiState.recentDocuments, 
                    onDocumentClick = { docId -> navController.navigate(Screen.PdfViewer.createRoute(docId)) },
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            AllDocuments(
                uiState.allDocuments, 
                onDocumentClick = { docId -> navController.navigate(Screen.PdfViewer.createRoute(docId)) },
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickStats(totalDocuments: Int, totalSize: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = totalDocuments.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(text = "Documents", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val sizeMb = totalSize / (1024 * 1024)
                Text(text = "$sizeMb MB", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(text = "Total Size", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RecentDocuments(documents: List<DocumentEntity>, onDocumentClick: (Int) -> Unit, viewModel: HomeViewModel) {
    Column {
        Text(
            text = "Recent Documents",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(documents) { document ->
                DocumentItem(document = document, onClick = { onDocumentClick(document.id) }, viewModel = viewModel, isRecent = true)
            }
        }
    }
}

@Composable
fun AllDocuments(documents: List<DocumentEntity>, onDocumentClick: (Int) -> Unit, viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "All Documents",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (documents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No documents found. Tap + to add.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(documents) { document ->
                    DocumentItem(document = document, onClick = { onDocumentClick(document.id) }, viewModel = viewModel, isRecent = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentItem(document: DocumentEntity, onClick: () -> Unit, viewModel: HomeViewModel, isRecent: Boolean) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(document.fileName) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameDocument(document, newName)
                    showRenameDialog = false
                }) { Text("Rename") }
            },
            dismissButton = {
                Button(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box {
        Card(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .then(
                    if (isRecent) Modifier.size(160.dp, 120.dp)
                    else Modifier.fillMaxWidth()
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = document.fileName,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (isRecent) 2 else 1,
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!isRecent) {
                    val sizeKb = document.fileSize / 1024
                    Text(
                        text = "$sizeKb KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    showMenu = false
                    showRenameDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    viewModel.deleteDocument(document)
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    showMenu = false
                    viewModel.shareDocument(document)
                }
            )
        }
    }
}
