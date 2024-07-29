package app.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun Spacer() {
    Div({
        style {
            height(1.r)
            flexShrink(0)
        }
    })
}

@Composable
fun HorizontalSpacer() {
    Div({
        style {
            width(1.r)
            flexShrink(0)
        }
    })
}
