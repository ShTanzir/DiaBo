package com.tanzir.diabo.ui.editor

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tanzir.diabo.data.local.entity.FileType
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub

/**
 * Real syntax-highlighting code editor, replacing the Phase 1 BasicTextField placeholder.
 *
 * - .java files -> sora-editor's built-in JavaLanguage (full Java-aware highlighting)
 * - .xml files  -> TextMate XML grammar (registered from assets/textmate — see setup note below)
 *
 * Setup note (one-time, per sora-editor docs): copy the `textmate` sample assets
 * (language configs + xml.tmLanguage.json + a color scheme json) into
 * `app/src/main/assets/textmate/` so FileProviderRegistry can resolve them at runtime.
 * Until those assets are added, XML files gracefully fall back to EMPTY_LANGUAGE
 * (no highlighting, but the editor still works — see the catch block below).
 */
@Composable
fun SoraCodeEditor(
    fileType: FileType,
    initialContent: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val latestOnChange = rememberUpdatedState(onContentChange)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CodeEditor(ctx).apply {
                setText(initialContent)
                applyDiaBoEditorDefaults(isDark)
                setLanguageFor(fileType, ctx)
                subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
                    latestOnChange.value(text.toString())
                }
            }
        },
        update = { editor ->
            // Only push external content changes (e.g. tab switch) — avoid fighting the
            // user's live cursor position on every recomposition.
            if (editor.text.toString() != initialContent) {
                editor.setText(initialContent)
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { /* CodeEditor releases its own resources on detach */ }
    }
}

private fun CodeEditor.applyDiaBoEditorDefaults(isDark: Boolean) {
    isWordwrap = false
    isLineNumberEnabled = true
    setTextSize(14f)
    colorScheme = if (isDark) SchemeDarcula() else SchemeGitHub()
    // Bracket/tag matching + current-line highlight are on by default in sora-editor.
}

private fun CodeEditor.setLanguageFor(fileType: FileType, context: Context) {
    when (fileType) {
        FileType.JAVA -> setEditorLanguage(JavaLanguage())
        FileType.XML -> setXmlLanguage(context)
        else -> setEditorLanguage(io.github.rosemoe.sora.lang.EmptyLanguage())
    }
}

private var textMateRegistered = false

/**
 * XML syntax highlighting via sora-editor's TextMate support needs grammar assets
 * (see the class doc comment above) AND the exact ThemeRegistry API for the installed
 * sora-editor version verified against real docs — attempting that without either
 * caused a compile error here previously, so this intentionally stays EmptyLanguage
 * (plain monospace, no highlighting) until both are set up. Java files are unaffected
 * and get full highlighting via JavaLanguage() above. Tracked in PHASE5_HARDENING.md.
 */
private fun CodeEditor.setXmlLanguage(context: Context) {
    runCatching {
        if (!textMateRegistered) {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))
            textMateRegistered = true
        }
        setEditorLanguage(io.github.rosemoe.sora.lang.EmptyLanguage())
    }.onFailure {
        setEditorLanguage(io.github.rosemoe.sora.lang.EmptyLanguage())
    }
}
