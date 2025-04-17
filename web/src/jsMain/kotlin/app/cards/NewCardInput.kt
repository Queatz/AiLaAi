package app.cards

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.nav.NavSearchInput
import appString
import components.IconButton
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
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
    onSubmit: (name: String, active: Boolean) -> Unit,
) {
    var newCardTitle by remember { mutableStateOf("") }
    var publishNow by remember { mutableStateOf(false) }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Row)
            flexShrink(0)
            alignItems("center")
        }
    }) {
        NavSearchInput(
            newCardTitle,
            { newCardTitle = it },
            placeholder = appString { newCard },
            autoFocus = false,
            defaultMargins = defaultMargins,
            styles = {
                flexGrow(1)
            }
        ) {
            if (newCardTitle.isNotBlank()) {
                onSubmit(it, publishNow)
                newCardTitle = ""
            }
        }

        IconButton(
            name = if (publishNow) "toggle_on" else "toggle_off",
            title = appString { publish },
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
