package app.widget.space

import Styles
import androidx.compose.runtime.Composable
import app.widget.WidgetStyles
import components.IconButton
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.dom.Div
import r

/**
 * Side panel that appears when content is selected in the Space Widget.
 * Provides controls for manipulating the selected content.
 */
@Composable
fun SpaceWidgetSidePanel(
    onSendToBack: () -> Unit,
    onBringToFront: () -> Unit,
) {
    Div(
        attrs = {
            classes(WidgetStyles.spaceSidePanel)
            style {
                position(Absolute)
                right(1.r)
                top(1.r)
                bottom(1.r)
                overflow("auto")
            }
        }
    ) {
        IconButton(
            name = "flip_to_back",
            // todo: translate
            title = "Send to back",
            styles = {
                borderRadius(1.r)
                backgroundColor(Styles.colors.primary)
            },
            onClick = {
                onSendToBack()
            }
        )

        IconButton(
            name = "flip_to_front",
            // todo: translate
            title = "Bring to front",
            styles = {
                borderRadius(1.r)
                backgroundColor(Styles.colors.primary)
            },
            onClick = {
                onBringToFront()
            }
        )
    }
}
