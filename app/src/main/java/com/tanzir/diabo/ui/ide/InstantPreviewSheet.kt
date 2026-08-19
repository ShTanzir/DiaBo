package com.tanzir.diabo.ui.ide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tanzir.diabo.preview.JavaClickSimulator
import com.tanzir.diabo.preview.JavaMethodExtractor
import com.tanzir.diabo.preview.ViewRenderResult
import com.tanzir.diabo.preview.XmlLayoutParser
import com.tanzir.diabo.preview.XmlParseResult
import com.tanzir.diabo.preview.XmlViewRenderer
import kotlinx.coroutines.delay

private const val DEBOUNCE_MS = 400L

/**
 * ⚡ Instant Preview — renders the currently-open XML layout as real Android Views,
 * refreshing automatically ~400ms after the user stops typing. Java `onClick` handlers
 * are simulated for a safe supported subset (Toast, setText, setVisibility); anything
 * else is surfaced as "not simulated" rather than silently ignored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantPreviewSheet(
    xmlContent: String?,
    javaContent: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Debounce: only re-render after the user pauses typing, so the preview never
    // fights the editor for CPU on every keystroke.
    var debouncedXml by remember { mutableStateOf(xmlContent) }
    LaunchedEffect(xmlContent) {
        delay(DEBOUNCE_MS)
        debouncedXml = xmlContent
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 560.dp)
                .padding(16.dp)
        ) {
            Text("Instant Preview", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            when {
                debouncedXml.isNullOrBlank() -> {
                    InfoState("Open a layout .xml file to see its preview here")
                }
                else -> {
                    val parseResult = remember(debouncedXml) { XmlLayoutParser.parse(debouncedXml!!) }
                    when (parseResult) {
                        is XmlParseResult.Error -> ErrorState(parseResult.message, parseResult.lineNumber)
                        is XmlParseResult.Success -> {
                            var lastClickedMethod by remember { mutableStateOf<String?>(null) }
                            val renderer = remember(context) {
                                XmlViewRenderer(context) { methodName -> lastClickedMethod = methodName }
                            }
                            val renderResult = remember(parseResult) { renderer.render(parseResult.root) }

                            when (renderResult) {
                                is ViewRenderResult.Error -> ErrorState(renderResult.message, renderResult.lineNumber)
                                is ViewRenderResult.Success -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .heightIn(min = 200.dp)
                                    ) {
                                        AndroidView(factory = { renderResult.view }, modifier = Modifier.fillMaxSize())
                                    }

                                    LaunchedEffect(lastClickedMethod) {
                                        val method = lastClickedMethod ?: return@LaunchedEffect
                                        val body = javaContent?.let { JavaMethodExtractor.extractMethodBody(it, method) }
                                        if (body != null) {
                                            JavaClickSimulator(renderResult.view).simulate(body)
                                        }
                                    }

                                    if (renderResult.warnings.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        WarningsList(renderResult.warnings)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoState(message: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorState(message: String, lineNumber: Int?) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text("Can't preview this layout yet", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (lineNumber != null) "Line $lineNumber: $message" else message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun WarningsList(warnings: List<String>) {
    Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 100.dp)) {
        warnings.forEach { w ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(top = 3.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(w, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
