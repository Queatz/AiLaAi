package app.components

import Styles
import androidx.compose.runtime.Composable
import com.queatz.db.Status
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun StatusName(status: Status, gap: CSSSizeValue<*> = .25.r) {
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
                backgroundColor(Color(status.color ?: "#ffffff"))
            }
        }) {}
        Span {
            Text(status.name!!)
        }
    }
}
