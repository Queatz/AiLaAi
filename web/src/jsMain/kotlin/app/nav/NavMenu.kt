package app.nav

import Strings.newStory
import Styles
import androidx.compose.runtime.Composable
import app.AppStyles
import app.messaages.inList
import appString
import components.Icon
import focusable
import notBlank
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import stories.storyStatus


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
