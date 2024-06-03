package app.nav

import Styles
import androidx.compose.runtime.Composable
import app.AppStyles
import app.messaages.inList
import components.Icon
import focusable
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r


@Composable
fun NavMenu(content: @Composable () -> Unit) {
    Div({
        style {
            overflowY("auto")
            overflowX("hidden")
            padding(1.r / 2)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        content()
    }
}

@Composable fun NavMenuItem(icon: String?, title: String, selected: Boolean = false, textIcon: Boolean = false, onClick: () -> Unit) {
    Div({
        classes(
            listOf(AppStyles.groupItem, AppStyles.navMenuItem) + if (selected) AppStyles.groupItemSelected.inList() else emptyList()
        )
        focusable()
        onClick {
            onClick()
        }
    }) {
        if (icon != null) {
            if (textIcon) {
                Span({
                    classes(Styles.textIcon)
                }) {
                    Text(icon)
                }
            } else {
                Icon(icon)
            }
        }
        Text(title)
    }
}
