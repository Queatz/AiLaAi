package app.widget.space

import androidx.compose.runtime.Composable
import app.widget.WidgetStyles
import appString
import notBlank
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun SpaceWidgetPath(
    path: List<SpacePathItem>,
    currentPath: SpacePathItem,
    onNavigate: (Int) -> Unit
) {
    Div(
        attrs = {
            classes(WidgetStyles.spacePathWidget)
        }
    ) {
        (path + currentPath).forEachIndexed { index, item ->
            if (index > 0) {
                Span(
                    attrs = {
                        style {
                            padding(.125.r)
                            fontWeight("bold")
                        }
                    }
                ) {
                    Text("/")
                }
            }
            Span(
                attrs = {
                    classes(WidgetStyles.spacePathItem)

                    style {
                        if (item == currentPath && path.isNotEmpty()) {
                            fontWeight("bold")
                        } else {
                            opacity(.5)
                        }
                    }

                    onClick {
                        if (index <= path.lastIndex) {
                            onNavigate(index)
                        }
                    }
                }
            ) {
                Text(
                    item.card.name?.notBlank ?: appString { newCard }
                )
            }
        }
    }
}
