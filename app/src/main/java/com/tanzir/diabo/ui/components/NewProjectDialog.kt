package com.tanzir.diabo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanzir.diabo.templates.ProjectTemplate
import com.tanzir.diabo.templates.TemplatePickerList

/**
 * Two-step new-project flow: name entry, then an optional template pick (Phase 4).
 */
@Composable
fun NewProjectDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, template: ProjectTemplate?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var selectedTemplate by remember { mutableStateOf<ProjectTemplate?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(if (step == 1) "New Project" else "Choose a Template") },
        text = {
            Column {
                if (step == 1) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project name") },
                        singleLine = true,
                        enabled = !isCreating,
                        isError = errorMessage != null
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TemplatePickerList(
                        selectedTemplateId = selectedTemplate?.id,
                        onSelect = { selectedTemplate = it },
                        modifier = Modifier.height(360.dp)
                    )
                }
                if (isCreating) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                TextButton(onClick = { step = 2 }, enabled = !isCreating && name.isNotBlank()) {
                    Text("Next")
                }
            } else {
                TextButton(onClick = { onConfirm(name, selectedTemplate) }, enabled = !isCreating) {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            if (step == 2) {
                TextButton(onClick = { step = 1 }, enabled = !isCreating) { Text("Back") }
            } else {
                TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
            }
        }
    )
}
