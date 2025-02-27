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
    object Box : SpaceWidgetTool()
    object Text : SpaceWidgetTool()
    object Circle : SpaceWidgetTool()
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
            name = "pan_tool_alt",
            // todo: translate
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
            name = "pen_size_2",
            // todo: translate
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
        IconButton(
            name = "crop_square",
            // todo: translate
            title = "Box",
            styles = {
                borderRadius(1.r)
                if (tool == SpaceWidgetTool.Box) {
                    backgroundColor(Styles.colors.primary)
                }
            },
            onClick = {
                onTool(SpaceWidgetTool.Box)
            }
        )
        IconButton(
            name = "circle",
            // todo: translate
            title = "Circle",
            styles = {
                borderRadius(1.r)
                if (tool == SpaceWidgetTool.Circle) {
                    backgroundColor(Styles.colors.primary)
                }
            },
            onClick = {
                onTool(SpaceWidgetTool.Circle)
            }
        )
        IconButton(
            name = "title",
            // todo: translate
            title = "Text",
            styles = {
                borderRadius(1.r)
                if (tool == SpaceWidgetTool.Text) {
                    backgroundColor(Styles.colors.primary)
                }
            },
            onClick = {
                onTool(SpaceWidgetTool.Text)
            }
        )
    }
}
