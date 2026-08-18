package com.tanzir.diabo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tanzir.diabo.ui.components.GlassCard

/**
 * Phase 3 adds GitHub Integration here — connecting the PAT + template repo
 * that "▶ Real Build" dispatches to. Everything else remains a Phase 4 stub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Storage", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("All projects are saved under the app's private DiaBo/ folder in internal storage.")
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GitHub Integration", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (state.isConfigured) {
                        AssistChip(onClick = {}, label = { Text("Connected") })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Used by \"▶ Real Build\" to trigger a cloud APK build via your DiaBo Preview Template repo.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.token,
                    onValueChange = { v -> viewModel.onFieldChange { it.copy(token = v) } },
                    label = { Text("Personal Access Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.owner,
                        onValueChange = { v -> viewModel.onFieldChange { it.copy(owner = v) } },
                        label = { Text("Owner") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.repo,
                        onValueChange = { v -> viewModel.onFieldChange { it.copy(repo = v) } },
                        label = { Text("Repo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.branch,
                        onValueChange = { v -> viewModel.onFieldChange { it.copy(branch = v) } },
                        label = { Text("Branch") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.workflowFile,
                        onValueChange = { v -> viewModel.onFieldChange { it.copy(workflowFile = v) } },
                        label = { Text("Workflow file") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row {
                    Button(onClick = viewModel::save) { Text("Save") }
                    Spacer(Modifier.width(8.dp))
                    if (state.isConfigured) {
                        OutlinedButton(onClick = viewModel::disconnect) { Text("Disconnect") }
                    }
                }

                state.savedMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
                ) { uri -> uri?.let { viewModel.exportBackup(it) } }
                val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri -> uri?.let { viewModel.restoreBackup(it) } }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("DiaBo_Backup_${System.currentTimeMillis()}.zip") }) {
                        Text("Export Backup")
                    }
                    OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }) {
                        Text("Restore Backup")
                    }
                }
                state.backupMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Language / ভাষা", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.isBangla,
                        onClick = { viewModel.setLanguage(bangla = false) },
                        label = { Text("English") }
                    )
                    FilterChip(
                        selected = state.isBangla,
                        onClick = { viewModel.setLanguage(bangla = true) },
                        label = { Text("বাংলা") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Coming in Phase 5", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Editor theme & font controls, full test coverage, crash reporting.")
            }
        }
    }
}
