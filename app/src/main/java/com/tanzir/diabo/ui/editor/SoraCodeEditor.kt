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
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
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
                subscribeEvent<io.github.rosemoe.sora.event.ContentChangeEvent> { _, _ ->
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
 * Registers the XML TextMate grammar once per process, then applies it.
 * Falls back to no-highlighting (EmptyLanguage) if the grammar assets aren't
 * present yet, so a missing asset never crashes the editor.
 */
private fun CodeEditor.setXmlLanguage(context: Context) {
    runCatching {
        if (!textMateRegistered) {
            FileProviderRegistry.getInstance().addFileProvider(
                AssetsFileResolver(context.assets)
            )
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            ThemeRegistry.getInstance().loadTheme(
                ThemeModel(
                    io.github.rosemoe.sora.langs.textmate.registry.model.ThemeSource("textmate/xml.json", "xml")
                )
            )
            textMateRegistered = true
        }
        setEditorLanguage(TextMateLanguage.create("source.xml", true))
    }.onFailure {
        setEditorLanguage(io.github.rosemoe.sora.lang.EmptyLanguage())
    }
}
