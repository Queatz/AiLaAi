package app.dialog

import app.AppStyles
import application
import focusable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun inputSelectDialog(
    confirmButton: String,
    items: List<String>? = null,
    itemStyle: StyleScope.(String) -> Unit = {}

) = inputDialog(
    null,
    confirmButton = confirmButton,
    placeholder = application.appString { search },
    inputStyles = {
        width(100.percent)
    }
) { resolve, value, onValue ->
    Div({
        style {
            overflowY("auto")
            maxHeight(16.r)
        }
    }) {
        items?.let {
            if (value.isNotBlank()) {
                it.filter { it.contains(value, ignoreCase = true) }
            } else {
                it
            }
        }?.forEach { item ->
            Div({
                classes(
                    listOf(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                )

                style {
                    itemStyle(item)
                    marginTop(.5.r)
                }

                onClick {
                    onValue(item)
                    resolve(true)
                }

                focusable()
            }) {
                Div {
                    Div({
                        classes(AppStyles.groupItemName)
                    }) {
                        Text(item)
                    }
                }
            }
        }
    }
}
