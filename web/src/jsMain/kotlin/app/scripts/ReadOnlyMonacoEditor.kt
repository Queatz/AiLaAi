package app.scripts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.compose.rememberDarkMode
import lib.Monaco
import lib.jsObject
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.dom.Div
import r

/**
 * A read-only version of MonacoEditor for displaying code without editing capabilities.
 */
@Composable
fun ReadOnlyMonacoEditor(
    initialValue: String = "",
    styles: StyleScope.() -> Unit = {},
) {
    var editor: Monaco.IEditor? by remember { mutableStateOf(null) }
    val darkMode = rememberDarkMode()

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

    Div({
        style {
            borderRadius(1.r)
            overflow("hidden")
            styles()
        }

        ref { container ->
            editor = Monaco.editor.create(
                container = container,
                options = jsObject {
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

            onDispose {
                editor?.dispose()
                editor = null
            }
        }
    })
}
