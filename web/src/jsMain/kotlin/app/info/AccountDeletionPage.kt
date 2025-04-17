package app.info

import androidx.compose.runtime.Composable
import appText
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun AccountDeletionPage() {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(1.r)
            alignItems(AlignItems.Stretch)
        }
    }) {
        H1 {
            appText { accountDeletion }
        }
        Div {
            A(href = "mailto:jacobaferrero@gmail.com") {
                appText { sendAnEmail }
            }
            Span {
                Text(" ")
                appText { sendAnEmailDeleteAccount }
            }
        }
        Div({
            style {
                marginTop(1.r)
            }
        }) {
            appText { sendAnEmailDeleteAccount2 }
        }
    }
}
