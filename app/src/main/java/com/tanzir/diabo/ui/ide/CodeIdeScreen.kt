package com.tanzir.diabo.ui.ide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeIdeScreen(
    onBack: () -> Unit,
    viewModel: CodeIdeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val previewXml by viewModel.activePreviewXml.collectAsState()
    val previewJava by viewModel.activePreviewJava.collectAsState()
    val realBuildState by viewModel.realBuildUiState.collectAsState()
    val trashedFiles by viewModel.trashedFiles.collectAsState()
    val isTrashVisible by viewModel.isTrashVisible.collectAsState()
    var showRunMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.project?.name ?: "DiaBo IDE", fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSidebar) {
                        Icon(Icons.Filled.Menu, contentDescription = "Toggle file explorer")
                    }
                    IconButton(onClick = viewModel::toggleTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = "Trash")
                    }
                    IconButton(onClick = { showRunMenu = true }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Run / Preview")
                    }
                    DropdownMenu(expanded = showRunMenu, onDismissRequest = { showRunMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("⚡ Instant Preview") },
                            onClick = { showRunMenu = false; viewModel.toggleInstantPreview() }
                        )
                        DropdownMenuItem(
                            text = { Text("▶ Real Build") },
                            onClick = { showRunMenu = false; viewModel.triggerRealBuild() }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (state.isSidebarOpen) {
                FileExplorerSidebar(
                    files = state.files,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onFileClick = viewModel::openFile,
                    onNewFile = { viewModel.requestNewEntry(NewEntryTarget.File) },
                    onNewFolder = { viewModel.requestNewEntry(NewEntryTarget.Folder) },
                    onImport = { /* Phase 1.1: ACTION_OPEN_DOCUMENT_TREE launcher hook */ },
                    onRename = viewModel::requestRename,
                    onDelete = viewModel::requestDelete,
                    onDuplicate = viewModel::duplicateFile,
                    onTogglePin = viewModel::togglePin
                )
                // Manually drawn (not Material3's VerticalDivider, which needs material3 1.3.0+
                // and may not be present in the pinned compose-bom version) — guaranteed to
                // compile regardless of the exact Material3 version resolved.
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            CodeEditorPanel(
                tabs = state.openTabs,
                activeTabIndex = state.activeTabIndex,
                onTabSelected = viewModel::setActiveTab,
                onTabClosed = viewModel::closeTab,
                onContentChange = viewModel::onEditorContentChange,
                isSaving = state.isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // New file/folder dialog
    state.newEntryTarget?.let { target ->
        val label = if (target is NewEntryTarget.File) "New file (e.g. Helper.java)" else "New folder name"
        SimpleTextDialog(
            title = if (target is NewEntryTarget.File) "New File" else "New Folder",
            label = label,
            onDismiss = viewModel::dismissNewEntry,
            onConfirm = viewModel::createEntry
        )
    }

    // Rename dialog
    state.renameTarget?.let { file ->
        SimpleTextDialog(
            title = "Rename",
            label = "New name",
            initialValue = file.name,
            onDismiss = viewModel::dismissRename,
            onConfirm = viewModel::confirmRename
        )
    }

    // Delete confirmation
    state.deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete '${file.name}'?") },
            text = { Text("You can restore this from Trash within 24 hours.") },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") } }
        )
    }

    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }
        )
    }

    if (state.showInstantPreview) {
        InstantPreviewSheet(
            xmlContent = previewXml,
            javaContent = previewJava,
            onDismiss = viewModel::dismissInstantPreview
        )
    }

    if (realBuildState.isVisible) {
        RealBuildSheet(
            state = realBuildState,
            onDismiss = viewModel::dismissRealBuild
        )
    }

    if (isTrashVisible) {
        TrashSheet(
            trashedFiles = trashedFiles,
            onRestore = viewModel::restoreFile,
            onDismiss = viewModel::dismissTrash
        )
    }
}

@Composable
private fun SimpleTextDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
