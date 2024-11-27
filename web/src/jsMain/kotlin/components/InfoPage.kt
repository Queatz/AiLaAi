package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.info.AccountDeletionPage
import app.info.UseCasesPage
import app.softwork.routingcompose.Router
import application
import mainContent
import org.jetbrains.compose.web.css.AlignSelf
import org.jetbrains.compose.web.css.alignSelf
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun InfoPage(page: String) {
    val layout by application.layout.collectAsState()

    Div({
        mainContent(layout)
    }) {
        Div({
            classes(Styles.navContainer)
            style {
                width(1200.px)
                flexShrink(1f)
                alignSelf(AlignSelf.Center)
                marginBottom(1.r)
            }
        }) {
            Div({
                classes(Styles.navContent)
                style {
                    padding(1.r)
                }
            }) {
                when (page) {
                    "contact" -> {
                        ContactPage()
                    }
                    "open-source" -> {
                        OpenSourcePage()
                    }
                    "use-cases" -> {
                        UseCasesPage()
                    }
                    "account-deletion" -> {
                        AccountDeletionPage()
                    }
                    else -> Router.current.navigate("/")
                }
            }
        }
    }
}
