package app.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import app.AppStyles
import application
import focusable
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r

suspend fun <T> inputWithListDialog(
    items: List<T>,
    selected: StateFlow<T?>,
    defaultValue: String = "",
    placeholder: String = "",
    confirmButton: String = application.appString { confirm },
    singleLine: Boolean = false,
    topContent: (@Composable (resolve: (Boolean?) -> Unit) -> Unit)? = null,
    extraButtons: (@Composable (resolve: (Boolean?) -> Unit) -> Unit)? = null,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    itemContent: @Composable (T) -> Unit
) = inputDialog(
    title = null,
    placeholder = placeholder,
    confirmButton = confirmButton,
    defaultValue = defaultValue,
    singleLine = singleLine,
    inputStyles = { width(32.r) },
    extraButtons = extraButtons
) { resolve, _, _ ->
    topContent?.invoke(resolve)
    Div({
        style {
            overflowY("auto")
            height(8.r)
        }
    }) {
        key(selected.collectAsState().value) {
            items.forEach { item ->
                Div({
                    classes(
                        listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                    )

                    if (isSelected(item)) {
                        classes(AppStyles.groupItemSelected)
                    }

                    onClick {
                        onSelect(item)
                    }

                    focusable()
                }) {
                    Div {
                        Div({
                            classes(AppStyles.groupItemName)
                        }) {
                            itemContent(item)
                        }
                    }
                }
            }
        }
    }
}
