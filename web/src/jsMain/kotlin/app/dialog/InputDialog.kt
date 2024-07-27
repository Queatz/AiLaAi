package app.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.components.TextBox
import app.nav.NavSearchInput
import application
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import r

suspend fun inputDialog(
    title: String?,
    placeholder: String = "",
    confirmButton: String = application.appString { okay },
    cancelButton: String? = application.appString { cancel },
    defaultValue: String = "",
    singleLine: Boolean = true,
    inputStyles: StyleScope.() -> Unit = {},
    actions: (@Composable (resolve: (Boolean?) -> Unit) -> Unit)? = null,
    content: @Composable (resolve: (Boolean?) -> Unit, value: String, onValue: (String) -> Unit) -> Unit = { _, _, _ -> }
): String? {
    var text: String = defaultValue
    val result = dialog(
        title = title,
        confirmButton = confirmButton,
        cancelButton = cancelButton,
        actions = actions
    ) { resolve ->
        var value by remember {
            mutableStateOf(defaultValue)
        }

        if (singleLine) {
            NavSearchInput(
                value,
                {
                    value = it
                    text = it
                },
                placeholder = placeholder,
                selectAll = true,
                styles = {
                    margin(0.r)
                    maxWidth(100.percent)
                    inputStyles()
                },
                onDismissRequest = {
                    resolve(false)
                }
            ) {
                resolve(true)
            }
        } else {
            TextBox(
                value,
                {
                    value = it
                    text = it
                },
                placeholder = placeholder,
                selectAll = false,
                styles = {
                    margin(0.r)
                    width(32.r)
                    maxWidth(100.percent)
                    inputStyles()
                },
            ) {
                resolve(true)
            }
        }
        content(resolve, value) {
            value = it
            text = it
        }
    }

    return if (result == true) text else null
}
