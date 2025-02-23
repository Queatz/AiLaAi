package app.widget.space

import androidx.compose.runtime.Composable
import app.widget.WidgetStyles
import components.IconButton
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.dom.Div
import r

sealed class SpaceWidgetTool {
    object Default : SpaceWidgetTool()
    object Line : SpaceWidgetTool()
}

@Composable
fun SpaceWidgetToolbar(
    tool: SpaceWidgetTool,
    onTool: (SpaceWidgetTool) -> Unit,
) {
    Div(
        attrs = {
            classes(WidgetStyles.spacePathToolbar)
        }
    ) {
        IconButton(
            // todo: translate
            name = "pan_tool_alt",
            title = "Default",
            styles = {
                borderRadius(1.r)
                if (tool == SpaceWidgetTool.Default) {
                    backgroundColor(Styles.colors.primary)
                }
            },
            onClick = {
                onTool(SpaceWidgetTool.Default)
            }
        )
        IconButton(
            // todo: translate
            name = "pen_size_2",
            title = "Line",
            styles = {
                borderRadius(1.r)
                if (tool == SpaceWidgetTool.Line) {
                    backgroundColor(Styles.colors.primary)
                }
            },
            onClick = {
                onTool(SpaceWidgetTool.Line)
            }
        )
    }
}
