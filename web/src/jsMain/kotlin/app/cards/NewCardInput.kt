package app.cards

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.components.FlexInput
import appString
import components.IconButton
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun NewCardInput(
    defaultMargins: Boolean = true,
    enabled: Boolean = true,
    styles: StyleScope.() -> Unit = {},
    onSubmit: (name: String, active: Boolean) -> Unit,
) {
    var newCardTitle by remember { mutableStateOf("") }
    var publishNow by remember { mutableStateOf(false) }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Row)
            alignItems("center")
            styles()
        }
    }) {
        FlexInput(
            value = newCardTitle,
            onChange = { newCardTitle = it },
            placeholder = appString { newCard },
            autoFocus = false,
            enabled = enabled,
            defaultMargins = defaultMargins,
            styles = {
                flexGrow(1)
            },
            onSubmit = {
                if (newCardTitle.isNotBlank()) {
                    onSubmit(newCardTitle, publishNow)
                    newCardTitle = ""
                }
                true
            }
        )

        IconButton(
            name = if (publishNow) "toggle_on" else "toggle_off",
            title = appString { publish },
            enabled = enabled,
            onClick = {
                publishNow = !publishNow
            },
            styles = {
                if (publishNow) {
                    color(Styles.colors.primary)
                } else {
                    color(Styles.colors.secondary)
                }

                marginTop(.5.r)

                if (!defaultMargins) {
                    marginLeft(1.r)
                } else {
                    marginRight(2.r)
                }
            }
        )
    }
}
