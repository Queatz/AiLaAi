package app.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun FlexColumns(
    columnCount: Int = 3,
    padding: CSSSizeValue<*> = 1.r,
    style: StyleScope.() -> Unit = {},
    columnStyle: StyleScope.(index: Int) -> Unit = {},
    content: @Composable (index: Int) -> Unit
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Row)
            gap(padding)
            boxSizing("border-box")
            overflowX("hidden")
            overflowY("auto")
            style()
        }
    }) {
        repeat(columnCount) { index ->
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    flex(1)
                    padding(padding)
                    boxSizing("border-box")
                    position(Position.Relative)
                    columnStyle(index)
                }
            }) {
                content(index)
            }
        }
    }
}
