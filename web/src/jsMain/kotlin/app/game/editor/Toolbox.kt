package app.game.editor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun Toolbox(
    styles: (StyleScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Div({
        style {
            maxHeight(32.r)
            overflow("auto")
            property("border-radius", .75.r)
            padding(.5.r)
            property("box-shadow", "0 0 .5rem #00000028 inset")
            styles?.invoke(this)
        }
    }) {
        content()
    }
}
