package com.tanzir.diabo.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanzir.diabo.ui.components.GlassCard

/**
 * Templates Gallery (PRD §7.7) — shown as a picker step inside "New Project".
 * Selecting a template seeds the new project's Java+XML instead of the blank default.
 */
@Composable
fun TemplatePickerList(
    selectedTemplateId: String?,
    onSelect: (ProjectTemplate?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TemplateRow(
                title = "Blank (no template)",
                description = "Start from an empty Hello World screen",
                isSelected = selectedTemplateId == null,
                onClick = { onSelect(null) }
            )
        }
        items(TemplateCatalog.all, key = { it.id }) { template ->
            TemplateRow(
                title = template.title,
                description = template.description,
                isSelected = selectedTemplateId == template.id,
                onClick = { onSelect(template) }
            )
        }
    }
}

@Composable
private fun TemplateRow(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isSelected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
