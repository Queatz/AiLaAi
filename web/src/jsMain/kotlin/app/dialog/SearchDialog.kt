package app.dialog

import Configuration
import LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.components.FlexInput
import application
import components.LazyColumn
import components.Loading
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r

suspend fun <T : Any> searchDialog(
    configuration: Configuration,
    title: String,
    placeholder: String = application.appString { search },
    confirmButton: String = application.appString { close },
    cancelButton: String? = null,
    defaultValue: String = "",
    load: suspend () -> List<T>,
    filter: (T, String) -> Boolean,
    item: @Composable (T, resolve: (Boolean) -> Unit) -> Unit,
) = dialog(title, confirmButton, cancelButton) { resolve ->
    var value by remember { mutableStateOf(defaultValue) }
    var loading by remember { mutableStateOf(true) }
    var allItems by remember { mutableStateOf(emptyList<T>()) }
    var items by remember { mutableStateOf(emptyList<T>()) }

    LaunchedEffect(Unit) {
        allItems = load()
        loading = false
    }

    LaunchedEffect(allItems, value) {
        items = if (value.isBlank()) {
            allItems
        } else {
            allItems.filter { filter(it, value) }
        }
    }

    CompositionLocalProvider(LocalConfiguration provides configuration) {
        FlexInput(
            value = value,
            onChange = {
                value = it
            },
            placeholder = placeholder,
            styles = {
                margin(0.r)
                width(28.r)
                maxWidth(100.percent)
            },
            selectAll = true,
            onDismissRequest = {
                resolve(false)
            },
            onSubmit = {
                resolve(true)
                true
            }
        )

        if (loading) {
            Div({
                style {
                    padding(1.r)
                }
            }) {
                Loading()
            }
        } else {
            Div({
                style {
                    overflowY("auto")
                    overflowX("hidden")
                    width(28.r)
                    maxWidth(100.percent)
                    padding(1.r / 2, 0.r)
                }
            }) {
                key(items) {
                    LazyColumn {
                        items(items) {
                            item(it, resolve)
                        }
                    }
                }
            }
        }
    }
}
