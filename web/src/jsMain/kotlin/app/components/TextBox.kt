package app.components

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.Event
import r

@Composable
fun TextBox(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String = "",
    selectAll: Boolean = false,
    styles: StyleScope.() -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    var onValueChange by remember { mutableStateOf({}) }

    LaunchedEffect(value) {
        onValueChange()
    }

    TextArea(value) {
        classes(Styles.textarea)
        style {
            height(3.5.r)
            maxHeight(18.r)
            flexShrink(0)
            backgroundColor(Color.transparent)
            styles()
        }

        placeholder(placeholder)

        onKeyDown {
            if (it.key == "Enter" && it.ctrlKey) {
                it.preventDefault()
                it.stopPropagation()
                onConfirm()
            }
        }

        onInput {
            onValue(it.value)
            it.target.style.height = "0"
            it.target.style.height = "${it.target.scrollHeight + 2}px"
        }

        onChange {
            it.target.style.height = "0"
            it.target.style.height = "${it.target.scrollHeight + 2}px"
        }

        ref { element ->
            element.style.height = "0"
            element.style.height = "${element.scrollHeight + 2}px"

            if (selectAll) {
                element.select()
            }

            onValueChange = { element.dispatchEvent(Event("change")) }

            onDispose {
                onValueChange = {}
            }
        }
    }
}
