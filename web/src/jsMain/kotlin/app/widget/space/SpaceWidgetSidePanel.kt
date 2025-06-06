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
    onFullscreen: () -> Unit,
    isFullscreen: Boolean = false,
    showContentTools: Boolean = false,
    onSendToBack: () -> Unit,
    onBringToFront: () -> Unit,
) {
    Div(
        attrs = {
            classes(WidgetStyles.spaceSidePanel)
            style {
                position(Absolute)
                right(0.r)
                top(0.r)
                bottom(0.r)
                overflow("auto")
                property("pointer-events", "none")
            }
        }
    ) {
        IconButton(
            name = if (isFullscreen) "fullscreen_exit" else "fullscreen",
            // todo: translate
            title = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
            background = true,
            styles = {
                borderRadius(1.r)
                property("pointer-events", "initial")
            },
            onClick = {
                onFullscreen()
            }
        )

        if (showContentTools) {
            IconButton(
                name = "flip_to_back",
                // todo: translate
                title = "Send to back",
                styles = {
                    borderRadius(1.r)
                    backgroundColor(Styles.colors.primary)
                    property("pointer-events", "initial")
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
                    property("pointer-events", "initial")
                },
                onClick = {
                    onBringToFront()
                }
            )
        }
    }
}
