package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import app.AppStyles
import focusable
import kotlinx.browser.window
import kotlinx.coroutines.awaitAnimationFrame
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.math.PI

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun IconButton(
    name: String,
    title: String,
    text: String? = null,
    count: Int = 0,
    background: Boolean = false,
    backgroundColor: CSSColorValue? = null,
    isLoading: Boolean = false,
    styles: (StyleScope.() -> Unit)? = null,
    iconStyles: (StyleScope.() -> Unit)? = null,
    onClick: (SyntheticMouseEvent) -> Unit
) {
    val start = remember { Clock.System.now().toEpochMilliseconds() }
    var rotation by remember { mutableStateOf(0.rad) }

    LaunchedEffect(isLoading) {
        if (isLoading) while (true) {
            rotation = ((Clock.System.now().toEpochMilliseconds() - start) / 2_000.0 * PI).rad
            delay(50)
            window.awaitAnimationFrame()
        }
    }

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

            style {
                iconStyles?.let {
                    it()
                }
                if (isLoading) {
                    property("font-smooth", "never")
                    transform {
                        rotate(rotation)
                    }
                }
            }
        }) {
            Text(if (isLoading) "progress_activity" else name)
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
fun Icon(
    name: String,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    styles: (StyleScope.() -> Unit)? = null
) {
    Span({
        classes("material-symbols-outlined")
        style {
            styles?.invoke(this)
        }

        if (title != null) {
            title(title)
        }

        if (onClick != null) {
            onClick {
                it.stopPropagation()
                onClick()
            }
        }
    }) {
        Text(name)
    }
}
