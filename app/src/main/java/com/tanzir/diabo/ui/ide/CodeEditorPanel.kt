package com.tanzir.diabo.ui.ide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Multi-tab code editor. Core editing surface is sora-editor (real Java/XML syntax
 * highlighting, code folding, auto-indent) wrapped by SoraCodeEditor — see
 * ui/editor/SoraCodeEditor.kt. This composable owns only the tab strip + saving indicator.
 */
@Composable
fun CodeEditorPanel(
    tabs: List<OpenTab>,
    activeTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onContentChange: (String) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Tab strip
        if (tabs.isNotEmpty()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                tabs.forEachIndexed { index, tab ->
                    EditorTabChip(
                        title = tab.file.name,
                        isDirty = tab.isDirty,
                        isActive = index == activeTabIndex,
                        onClick = { onTabSelected(index) },
                        onClose = { onTabClosed(index) }
                    )
                }
            }
            Divider()
        }

        val activeTab = tabs.getOrNull(activeTabIndex)
        if (activeTab == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Open a file from the sidebar to start editing", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                // key() on file.id: forces sora-editor to rebuild (and reset undo history)
                // when the user switches tabs, instead of reusing one CodeEditor's state.
                androidx.compose.runtime.key(activeTab.file.id) {
                    com.tanzir.diabo.ui.editor.SoraCodeEditor(
                        fileType = activeTab.file.type,
                        initialContent = activeTab.content,
                        onContentChange = onContentChange,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isSaving) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Saving…", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTabChip(title: String, isDirty: Boolean, isActive: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.clickableSimple(onClick),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        if (isDirty) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Circle, contentDescription = "Unsaved changes", modifier = Modifier.size(8.dp))
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Filled.Close,
            contentDescription = "Close tab",
            modifier = Modifier.size(16.dp).clickableSimple(onClose)
        )
    }
}

// Small helper to attach a plain click without importing extra boilerplate at every call site.
private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
