package com.tanzir.diabo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tanzir.diabo.data.local.entity.Project
import com.tanzir.diabo.ui.components.GlassCard
import com.tanzir.diabo.ui.components.NewProjectDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (String) -> Unit,
    onSeeAllProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("DiaBo", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::onFabClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Project") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Quick stats
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "Projects", value = state.totalProjects.toString())
                    StatItem(label = "Builds this month", value = "0") // wired up in Phase 3
                    StatItem(label = "Last build", value = "—")
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Projects", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onSeeAllProjects) { Text("See all") }
            }

            Spacer(Modifier.height(8.dp))

            if (state.recentProjects.isEmpty()) {
                EmptyHomeState(onCreateClick = viewModel::onFabClick)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recentProjects, key = { it.id }) { project ->
                        RecentProjectCard(project = project, onClick = { onOpenProject(project.id) })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Import Project", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Bring in files/folders from device storage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = onSeeAllProjects) { Text("Open") }
                }
            }
        }
    }

    if (state.showNewProjectDialog) {
        NewProjectDialog(
            isCreating = state.isCreatingProject,
            errorMessage = state.createProjectError,
            onDismiss = viewModel::dismissNewProjectDialog,
            onConfirm = { name, template -> viewModel.createProject(name, template, onCreated = onOpenProject) }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RecentProjectCard(project: Project, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null)
        Spacer(Modifier.height(8.dp))
        Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(project.packageName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Composable
private fun EmptyHomeState(onCreateClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("No projects yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap the button below to build your first Android app",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreateClick) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Create Project")
            }
        }
    }
}

