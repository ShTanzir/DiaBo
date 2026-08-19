package com.tanzir.diabo.ui.ide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanzir.diabo.data.local.entity.ProjectFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashSheet(
    trashedFiles: List<ProjectFile>,
    onRestore: (ProjectFile) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Trash", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Deleted files stay here for 24 hours before being permanently removed.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))

            if (trashedFiles.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Trash is empty", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(trashedFiles, key = { it.id }) { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(file.name, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRestore(file) }) {
                                Icon(Icons.Filled.Restore, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }
}
