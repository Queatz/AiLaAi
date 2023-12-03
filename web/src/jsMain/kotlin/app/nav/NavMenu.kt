package app.nav

import Styles
import androidx.compose.runtime.Composable
import app.AppStyles
import app.messaages.inList
import components.Icon
import focusable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

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
