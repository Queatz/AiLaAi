package components

import androidx.compose.runtime.Composable
import androidx.compose.web.events.SyntheticMouseEvent
import app.AppStyles
import focusable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun IconButton(
    name: String,
    title: String,
    text: String? = null,
    count: Int = 0,
    background: Boolean = false,
    backgroundColor: CSSColorValue? = null,
    styles: (StyleScope.() -> Unit)? = null,
    iconStyles: (StyleScope.() -> Unit)? = null,
    onClick: (SyntheticMouseEvent) -> Unit
) {
    Span({
        classes(AppStyles.iconButton)

        if (background) {
            classes(AppStyles.iconButtonBackground)
        }

        focusable()
        style {
            if (backgroundColor != null) {
                backgroundColor(backgroundColor)
            }

            if (text != null) {
                paddingLeft(1.r)
                paddingRight(1.r)
            }

            styles?.invoke(this)
        }
        title(title)
        onClick {
            it.stopPropagation()
            onClick(it)
        }
    }) {
        Span({
            classes("material-symbols-outlined")

            iconStyles?.let {
                style {
                    it()
                }
            }
        }) {
            Text(name)
        }
        if (text != null) {
            Span({
                style {
                    marginLeft(.5.r)
                }
            }) {
                Text(text)
            }
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
