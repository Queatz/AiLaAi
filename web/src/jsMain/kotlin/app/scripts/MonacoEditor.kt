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

@Composable
fun MonacoEditor(
    initialValue: String = "",
    onValueChange: (String) -> Unit = {},
    styles: StyleScope.() -> Unit = {},
) {
    var editor: Monaco.IEditor? by remember { mutableStateOf(null) }
    val darkMode = rememberDarkMode()

    LaunchedEffect(darkMode) {
        editor?.updateOptions(
            jsObject<Monaco.EditorOptions> {
                theme = if (darkMode) "vs-dark" else "vs-light"
            }
        )
    }

    LaunchedEffect(editor, initialValue) {
        editor?.apply {
            setValue(initialValue)
            setScrollTop(0)
            onDidChangeModelContent {
                onValueChange(getValue())
            }
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
                options = jsObject<Monaco.EditorOptions> {
                    value = initialValue
                    language = "kotlin"
                    theme = if (darkMode) "vs-dark" else "vs-light"
                    automaticLayout = true
                }
            )

            onDispose {
                editor?.dispose()
                editor = null
            }
        }
    })
}
