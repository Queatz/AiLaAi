package components

import androidx.compose.runtime.Composable
import androidx.compose.web.events.SyntheticMouseEvent
import app.AppStyles
import focusable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun IconButton(
    name: String,
    title: String,
    count: Int = 0,
    styles: (StyleScope.() -> Unit)? = null,
    onClick: (SyntheticMouseEvent) -> Unit
) {
    Span({
        classes(AppStyles.iconButton)
        focusable()
        style {
            styles?.invoke(this)
        }
        title(title)
        onClick {
            onClick(it)
        }
    }) {
        Span({
            classes("material-symbols-outlined")
        }) {
            Text(name)
        }
        if (count > 0) {
            Div({
                classes(AppStyles.iconButtonCount)
            }) {
                Text("${count.coerceAtMost(99)}")
            }
        }
    }
}

@Composable
fun Icon(name: String, title: String? = null, styles: (StyleScope.() -> Unit)? = null) {
    Span({
        classes("material-symbols-outlined")
        style {
            styles?.invoke(this)
        }

        if (title != null) {
            title(title)
        }
    }) {
        Text(name)
    }
}
