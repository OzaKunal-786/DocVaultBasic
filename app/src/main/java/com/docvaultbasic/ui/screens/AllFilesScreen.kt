package com.docvaultbasic.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.docvaultbasic.data.database.DocumentEntity
import com.docvaultbasic.ui.navigation.Screen
import com.docvaultbasic.ui.viewmodel.HomeViewModel
import com.docvaultbasic.util.NavigationUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class SortBy {
    NAME, SIZE, DATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFilesScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var sortBy by remember { mutableStateOf(SortBy.DATE) }
    var isAscending by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Bulk Delete State
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(isSelectionMode) {
        selectedIds = emptySet()
    }

    val filteredDocuments = remember(uiState.allDocuments, searchQuery) {
        if (searchQuery.isEmpty()) {
            uiState.allDocuments
        } else {
            uiState.allDocuments.filter { 
                it.fileName.contains(searchQuery, ignoreCase = true) || 
                it.folderSource.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val sortedDocuments = remember(filteredDocuments, sortBy, isAscending) {
        when (sortBy) {
            SortBy.NAME -> if (isAscending) filteredDocuments.sortedBy { it.fileName.lowercase() } else filteredDocuments.sortedByDescending { it.fileName.lowercase() }
            SortBy.SIZE -> if (isAscending) filteredDocuments.sortedBy { it.fileSize } else filteredDocuments.sortedByDescending { it.fileSize }
            SortBy.DATE -> if (isAscending) filteredDocuments.sortedBy { it.dateAdded } else filteredDocuments.sortedByDescending { it.dateAdded }
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            sortedDocuments.filter { it.id in selectedIds }.forEach {
                                viewModel.deleteDocument(it)
                            }
                            selectedIds = emptySet()
                            Toast.makeText(context, "Deleted selected files", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search documents...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    isSearchActive = false 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            searchQuery = ""
                            isSearchActive = false 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("All Files") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSearchActive && !isSelectionMode) {
                FloatingActionButton(
                    onClick = { isSearchActive = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search Files")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dropdown menu for sort (if not search/selection)
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { sortBy = SortBy.NAME; showSortMenu = false })
                DropdownMenuItem(text = { Text("Sort by Size") }, onClick = { sortBy = SortBy.SIZE; showSortMenu = false })
                DropdownMenuItem(text = { Text("Sort by Date") }, onClick = { sortBy = SortBy.DATE; showSortMenu = false })
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (isAscending) "Descending" else "Ascending") },
                    onClick = { isAscending = !isAscending; showSortMenu = false }
                )
            }

            if (!isSearchActive && !isSelectionMode) {
                // Path / Breadcrumb row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                    Text(text = "Vault", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                    Text(text = "All Documents", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (sortedDocuments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "No documents found" else "No matches for \"$searchQuery\"", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(sortedDocuments) { document ->
                        val isSelected = document.id in selectedIds
                        DocumentListItemNew(
                            document = document,
                            isSelected = isSelected,
                            onEdit = { NavigationUtils.editDocument(context, navController, document) },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) selectedIds - document.id else selectedIds + document.id
                                } else {
                                    NavigationUtils.openDocument(context, navController, document)
                                }
                            },
                            onLongClick = {
                                selectedIds = selectedIds + document.id
                            },
                            viewModel = viewModel
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentListItemNew(
    document: DocumentEntity,
    isSelected: Boolean,
    onEdit: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy h:mm a", Locale.getDefault()) }
    val dateString = remember(document.dateAdded) { dateFormat.format(Date(document.dateAdded)) }
    val fileSizeString = remember(document.fileSize) { 
        if (document.fileSize > 1024 * 1024) {
            String.format(Locale.getDefault(), "%.1f MB", document.fileSize / (1024.0 * 1024.0))
        } else {
            "${document.fileSize / 1024} KB"
        }
    }
    
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(document.fileName) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Document name", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDocument(document, newName)
                        showRenameDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Rename", fontSize = 13.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRenameDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }

    Box(modifier = Modifier.background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    } else {
                        Icon(
                            if (document.storedPath.lowercase().endsWith(".pdf") || document.originalPath.lowercase().endsWith(".pdf")) 
                                Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(text = fileSizeString, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
            
            if (!isSelected) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; showRenameDialog = true }, leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) })
            DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; viewModel.shareDocument(document) }, leadingIcon = { Icon(Icons.Default.Share, null) })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; viewModel.deleteDocument(document) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) })
        }
    }
}
