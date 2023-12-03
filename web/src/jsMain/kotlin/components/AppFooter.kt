package components

import Styles
import androidx.compose.runtime.Composable
import app.softwork.routingcompose.Router
import appString
import appText
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun AppFooter() {
    val router = Router.current
    Div({
        classes(Styles.appFooter)
        style {
            whiteSpace("pre-wrap")
        }
    }) {
        Span({
            classes(Styles.menuButton)
        }) {
            Span { Text(appString { madeWith }) }
            Span({
                style { color(Color.red) }
            }) { Text(" ♥ ") }
            Span { appText { inHCMC } }
        }
        Bullet()
        val cardId = appString { introductionCardId }
        Div({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/page/$cardId")
            }
        }) {
            appText { introduction }
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/info/contact")
            }
        }) {
            appText { contact }
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/privacy")
            }
        }) {
            appText { privacyPolicy }
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/terms")
            }
        }) {
            appText { tos }
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/info/open-source")
            }
        }) {
            appText { openSource }
        }
    }
}
