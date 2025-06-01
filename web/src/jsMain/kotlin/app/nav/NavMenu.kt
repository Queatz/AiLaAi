package app.nav

import Styles
import androidx.compose.runtime.Composable
import app.AppStyles
import app.messaages.inList
import components.Icon
import focusable
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text


@Composable
fun NavMenu(content: @Composable () -> Unit) {
    Div({
        classes(AppStyles.navMenu)
    }) {
        content()
    }
}

@Composable
fun NavSectionHeader(text: String) {
    Div({
        classes(AppStyles.navMenuSectionHeader)
    }) {
        Text(text)
    }
}

@Composable
fun NavMenuItem(
    icon: String?,
    title: String,
    description: String = "",
    selected: Boolean = false,
    textIcon: Boolean = false,
    iconColor: CSSColorValue? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Div({
        classes(
            listOf(
                AppStyles.groupItem,
                AppStyles.navMenuItem
            ) + if (selected) {
                AppStyles.groupItemSelected.inList()
            } else {
                emptyList()
            }
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

                    style {
                        if (iconColor != null) {
                            color(iconColor)
                        }
                    }
                }) {
                    Text(icon)
                }
            } else {
                Icon(icon) {
                    if (iconColor != null) {
                        color(iconColor)
                    }
                }
            }
        }

        Div({
            style {
                width(0.px)
                flexGrow(1)
            }
        }) {
            Div({
                classes(AppStyles.groupItemName)
            }) {
                Text(title)
            }
            if (description.isNotBlank()) {
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(description)
                }
            }
        }

        trailingIcon?.invoke()
    }
}
