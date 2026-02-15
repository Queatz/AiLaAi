package app.group

import Styles
import androidx.compose.runtime.Composable
import com.queatz.db.GroupExtended
import components.Icon
import components.IconButton
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun GroupSidePanel(group: GroupExtended) {
    Div({
        classes(Styles.pane, Styles.sidePane)
        style {
            padding(1.r)
            boxSizing("border-box")
        }
    }) {
        IconButton(
            name = "close",
            title = "Tasks",
            text = "Tasks",
            isReversed = true,
            background = true,
            styles = {
                margin(.5.r, 0.r)
                justifyContent(JustifyContent.SpaceBetween)
            }
        ) {
            // todo
        }
    }
}
