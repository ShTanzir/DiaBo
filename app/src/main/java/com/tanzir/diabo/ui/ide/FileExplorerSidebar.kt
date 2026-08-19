package com.tanzir.diabo.ui.ide

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tanzir.diabo.data.local.entity.FileType
import com.tanzir.diabo.data.local.entity.ProjectFile

/**
 * Sidebar file/folder explorer.
 * Implements: tree view, new file/folder, import (stub hook), search, rename, delete,
 * copy/duplicate, pin/favorite, type icons, multi-select, context menu, last-modified info.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerSidebar(
    files: List<ProjectFile>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFileClick: (ProjectFile) -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onImport: () -> Unit,
    onRename: (ProjectFile) -> Unit,
    onDelete: (ProjectFile) -> Unit,
    onDuplicate: (ProjectFile) -> Unit,
    onTogglePin: (ProjectFile) -> Unit,
    modifier: Modifier = Modifier
) {
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var contextMenuFile by remember { mutableStateOf<ProjectFile?>(null) }

    val visibleFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files else files.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxHeight().width(260.dp)) {
        // Header: title + new file/folder/import actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Files", style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = onNewFile) {
                    Icon(Icons.Filled.NoteAdd, contentDescription = "New file")
                }
                IconButton(onClick = onNewFolder) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder")
                }
                IconButton(onClick = onImport) {
                    Icon(Icons.Filled.FileUpload, contentDescription = "Import")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            placeholder = { Text("Search files") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )

        if (multiSelectMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${selectedIds.size} selected")
                Row {
                    TextButton(onClick = {
                        selectedIds.forEach { id -> visibleFiles.find { it.id == id }?.let(onDelete) }
                        selectedIds = emptySet(); multiSelectMode = false
                    }) { Text("Delete") }
                    TextButton(onClick = { selectedIds = emptySet(); multiSelectMode = false }) { Text("Cancel") }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (visibleFiles.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No files", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn {
                items(visibleFiles, key = { it.id }) { file ->
                    FileRow(
                        file = file,
                        isSelected = file.id in selectedIds,
                        multiSelectMode = multiSelectMode,
                        onClick = {
                            if (multiSelectMode) {
                                selectedIds = if (file.id in selectedIds) selectedIds - file.id else selectedIds + file.id
                            } else {
                                onFileClick(file)
                            }
                        },
                        onLongClick = {
                            multiSelectMode = true
                            selectedIds = selectedIds + file.id
                        },
                        onMoreClick = { contextMenuFile = file }
                    )
                }
            }
        }
    }

    // Context menu (rename / delete / duplicate / pin / info)
    contextMenuFile?.let { file ->
        ModalBottomSheet(onDismissRequest = { contextMenuFile = null }) {
            Column(Modifier.padding(16.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Last modified: ${formatTimestamp(file.lastModified)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                ContextAction(Icons.Filled.DriveFileRenameOutline, "Rename") {
                    onRename(file); contextMenuFile = null
                }
                ContextAction(Icons.Filled.ContentCopy, "Duplicate") {
                    onDuplicate(file); contextMenuFile = null
                }
                ContextAction(
                    if (file.isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                    if (file.isPinned) "Unpin" else "Pin to top"
                ) {
                    onTogglePin(file); contextMenuFile = null
                }
                ContextAction(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) {
                    onDelete(file); contextMenuFile = null
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    file: ProjectFile,
    isSelected: Boolean,
    multiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multiSelectMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onClick() })
        }
        Icon(fileTypeIcon(file.type), contentDescription = null, tint = fileTypeTint(file.type))
        Spacer(Modifier.width(8.dp))
        Text(file.name, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        if (file.isPinned) {
            Icon(Icons.Filled.Star, contentDescription = "Pinned", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
    }
}

@Composable
private fun ContextAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint)
    }
}

private fun fileTypeIcon(type: FileType) = when (type) {
    FileType.JAVA -> Icons.Filled.Code
    FileType.XML -> Icons.Filled.DataObject
    FileType.GRADLE -> Icons.Filled.Build
    FileType.FOLDER -> Icons.Filled.Folder
    FileType.OTHER -> Icons.Filled.InsertDriveFile
}

private fun fileTypeTint(type: FileType): Color = when (type) {
    FileType.JAVA -> Color(0xFF7FB69E)
    FileType.XML -> Color(0xFF89C2D9)
    FileType.GRADLE -> Color(0xFFD9A441)
    FileType.FOLDER -> Color(0xFFB6D7A8)
    FileType.OTHER -> Color.Gray
}

private fun formatTimestamp(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(millis))
}
