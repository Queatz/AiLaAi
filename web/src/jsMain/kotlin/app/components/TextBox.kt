package app.components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.Event
import r
import resize

@Composable
fun TextBox(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String = "",
    selectAll: Boolean = false,
    inline: Boolean = false,
    styles: StyleScope.() -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    var onValueChange by remember { mutableStateOf({}) }

    LaunchedEffect(value) {
        onValueChange()
    }

    TextArea(value) {
        if (inline) {
            classes(Styles.textarea, Styles.textareaInline)
        } else {
            classes(Styles.textarea)
        }
        style {
            height(3.5.r)
            if (!inline) {
                maxHeight(18.r)
            }
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
            it.target.resize()
        }

        onChange {
            it.target.resize()
        }

        ref { element ->
            element.resize()

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
