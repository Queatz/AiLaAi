package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cards.CardsPageStyles.layout
import app.softwork.routingcompose.Router
import application
import mainContent
import org.jetbrains.compose.web.css.*
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
                    else -> Router.current.navigate("/")
                }
            }
        }
    }
}
