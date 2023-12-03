package components

import Styles
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun SearchField(
    value: String,
    placeholder: String,
    focus: Boolean = true,
    styles: StyleScope.() -> Unit = {},
    onValue: (String) -> Unit
) {
    Div({
        style {
            position(Position.Relative)
            styles()
        }
    }) {
        Input(InputType.Text) {
            classes(Styles.textarea)
            style {
                width(100.percent)
                paddingLeft(3.r)
            }

            if (value.isNotEmpty()) {
                defaultValue(value)
            }

            placeholder(placeholder)

            onInput {
                onValue(it.value)
            }

            if (focus) {
                autoFocus()
            }
        }
        Span({
            classes("material-symbols-outlined")
            style {
                position(Position.Absolute)
                property("z-index", "1")
                property("pointer-events", "none")
                color(Styles.colors.primary)
                left(1.r)
                top(0.r)
                bottom(0.r)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
            }
        }) {
            Text("search")
        }
    }
}
