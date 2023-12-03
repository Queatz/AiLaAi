package components

import androidx.compose.runtime.Composable
import appText
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun ContactPage() {
    Div {
        appText { isCreatedBy }
        A(href = "/jacobferrero") {
            Text("Jacob Ferrero")
        }
        Text(".")
        Br()
        Br()
        appText { please }
        A("mailto:jacobaferrero@gmail.com") {
            appText { sendMeAnEmail }
        }
        appText { forAllInquiries }
    }
}
