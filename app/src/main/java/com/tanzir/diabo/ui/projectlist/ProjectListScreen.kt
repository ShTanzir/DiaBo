package com.tanzir.diabo.ui.projectlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onOpenProject: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (state.isSelectionMode) {
                            Text("${state.selectedIds.size} selected")
                        } else {
                            Text("Projects", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (state.isSelectionMode) viewModel.clearSelection() else onBack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (state.isSelectionMode) {
                            IconButton(onClick = viewModel::deleteSelected) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                            }
                        } else {
                            IconButton(onClick = viewModel::onViewModeToggle) {
                                Icon(
                                    if (state.viewMode == ViewMode.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                                    contentDescription = "Toggle view"
                                )
                            }
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Name") }, onClick = {
                                    viewModel.onSortOptionChange(SortOption.NAME); showSortMenu = false
                                })
                                DropdownMenuItem(text = { Text("Last Modified") }, onClick = {
                                    viewModel.onSortOptionChange(SortOption.LAST_MODIFIED); showSortMenu = false
                                })
                                DropdownMenuItem(text = { Text("Created Date") }, onClick = {
                                    viewModel.onSortOptionChange(SortOption.CREATED_DATE); showSortMenu = false
                                })
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search projects") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        }
    ) { padding ->
        if (state.projects.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No projects found")
            }
        } else {
            LazyVerticalGrid(
                columns = if (state.viewMode == ViewMode.GRID) GridCells.Fixed(2) else GridCells.Fixed(1),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.projects, key = { it.id }) { project ->
                    ProjectGridCard(
                        project = project,
                        isSelected = project.id in state.selectedIds,
                        isSelectionMode = state.isSelectionMode,
                        onClick = {
                            if (state.isSelectionMode) viewModel.toggleSelection(project.id)
                            else onOpenProject(project.id)
                        },
                        onLongClick = { viewModel.toggleSelection(project.id) },
                        onDuplicate = { viewModel.duplicateProject(project) },
                        onDelete = { viewModel.requestDelete(project) },
                        onRename = { renameTarget = project }
                    )
                }
            }
        }
    }

    state.pendingDeleteProject?.let { project ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete '${project.name}'?") },
            text = { Text("This permanently removes the project and all its files.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            }
        )
    }

    renameTarget?.let { project ->
        var newName by remember(project.id) { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameProject(project, newName)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }

    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Something went wrong") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }
        )
    }
}

@Composable
private fun ProjectGridCard(
    project: Project,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Folder,
                contentDescription = null
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(project.packageName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)

        Spacer(Modifier.height(8.dp))
        AssistChip(
            onClick = {},
            label = { Text(buildStatusLabel(project.lastBuildStatus.name)) }
        )
    }
}

private fun buildStatusLabel(status: String) = when (status) {
    "SUCCESS" -> "✅ Built"
    "FAILED" -> "❌ Failed"
    "IN_PROGRESS" -> "⏳ Building"
    else -> "— Never built"
}
