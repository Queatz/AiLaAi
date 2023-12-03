package app.dialog

import Configuration
import LocalConfiguration
import androidx.compose.runtime.*
import app.nav.NavSearchInput
import appString
import application
import components.Loading
import opensavvy.compose.lazy.LazyColumn
import org.jetbrains.compose.web.css.*
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
        NavSearchInput(
            value,
            {
                value = it
            },
            placeholder = appString { search },
            styles = {
                margin(0.r)
                width(28.r)
                maxWidth(100.percent)
            },
            selectAll = true,
            onDismissRequest = {
                resolve(false)
            }
        ) {
            resolve(true)
        }

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
