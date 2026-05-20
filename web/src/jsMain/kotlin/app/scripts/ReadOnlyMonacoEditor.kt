package app.scripts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.compose.rememberDarkMode
import kotlinx.coroutines.await
import lib.Monaco
import lib.jsObject
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import r

/**
 * A read-only version of MonacoEditor for displaying code without editing capabilities.
 */
@Composable
fun ReadOnlyMonacoEditor(
    initialValue: String = "",
    styles: StyleScope.() -> Unit = {},
) {
    var monacoModule by remember { mutableStateOf<Monaco?>(null) }
    var editor by remember { mutableStateOf<Monaco.IEditor?>(null) }
    val darkMode = rememberDarkMode()

    LaunchedEffect(Unit) {
        monacoModule = js("import('monaco-editor')").unsafeCast<kotlin.js.Promise<Monaco>>().await()
    }

    LaunchedEffect(editor, darkMode) {
        editor?.updateOptions(
            jsObject {
                theme = if (darkMode) "vs-dark" else "vs-light"
            }
        )
    }

    LaunchedEffect(editor, initialValue) {
        editor?.apply {
            setValue(initialValue)
            setScrollTop(0)
        }
    }

    val container = remember { mutableStateOf<HTMLElement?>(null) }

    LaunchedEffect(container.value, monacoModule, darkMode) {
        val c = container.value
        val m = monacoModule
        if (c != null && m != null && editor == null) {
            editor = m.editor.create(
                c,
                jsObject {
                    this.value = initialValue
                    this.language = "kotlin"
                    this.theme = if (darkMode) "vs-dark" else "vs-light"
                    this.automaticLayout = true
                    this.asDynamic().readOnly = true // Set editor to read-only
                    this.asDynamic().minimap = jsObject {
                        this.enabled = true
                    }
                }
            )
        }
    }

    Div({
        style {
            borderRadius(1.r)
            overflow("hidden")
            styles()
        }

        ref {
            container.value = it

            onDispose {
                container.value = null
                editor?.dispose()
                editor = null
            }
        }
    })
}
