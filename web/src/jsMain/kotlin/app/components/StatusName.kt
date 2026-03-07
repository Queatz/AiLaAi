package app.components

import Styles
import androidx.compose.runtime.Composable
import com.queatz.db.Status
import application
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun StatusName(status: Status?, gap: CSSSizeValue<*> = .25.r) {
    Div({
        style {
            display(DisplayStyle.Flex)
            gap(gap)
            alignItems(AlignItems.Center)
            fontSize(12.px)
        }
    }) {
        Div({
            classes(Styles.personItemStatusIndicator)

            style {
                if (status?.color != null) {
                    backgroundColor(Color(status.color!!))
                } else {
                    border(1.px, LineStyle.Solid, Color("#888888"))
                }
            }
        }) {}
        Span {
            Text(status?.name ?: application.appString { custom })
        }
    }
}
