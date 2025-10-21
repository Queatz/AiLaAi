package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.dialog.dialog
import app.softwork.routingcompose.Router
import appString
import appText
import application
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun AppFooter(
    showHome: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val router = Router.current
    Div({
        classes(Styles.appFooter)
    }) {
        if (showHome) {
            Span({
                classes(Styles.menuButton)
                onClick {
                    router.navigate("/")
                }
            }) {
                appText { appName }
            }
            Bullet()
        }
        Span({
            classes(Styles.menuButton)
            onClick {
                window.open("https://www.youtube.com/@hitownchat", target = "_blank")
            }
        }) {
            Text("YouTube")
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/info/use-cases")
            }
        }) {
            Text("Use Cases")
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                window.open("/Hi Town Whitepaper.pdf", target = "_blank")
            }
        }) {
            Text("Whitepaper")
        }
        Bullet()
        Div({
            classes(Styles.menuButton)
            onClick {
                scope.launch {
                    dialog(
                        title = "Investors",
                        cancelButton = null
                    ) {
                        Text("Hi Town is open to speaking with investors who have a valid use for the platform.")

                        Div({
                            style {
                                marginTop(1.r)
                            }
                        }) {
                            A(href = "mailto:jacobaferrero@gmail.com") {
                                Text("Send an email")
                            }
                        }
                    }
                }
            }
        }) {
            Text("Investors")
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
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                application.toggleLayout()
            }
        }) {
            Text("Toggle Layout")
        }
        Bullet()
        Span({
            classes(Styles.menuButton)
            onClick {
                router.navigate("/theme")
            }
        }) {
            Text("Theme")
        }
        Bullet()
        Span({
            classes(Styles.menuButton)

            onClick {
                scope.launch {
                    dialog(
                        title = "Chào bạn! \uD83D\uDC4B\uD83C\uDFFC",
                        cancelButton = null,
                        confirmButton = "Đi luôn"
                    ) {
                        Text("Đi cà phê hổng?")
                    }
                }
            }
        }) {
            Span { appText { madeWith } }
            Span({
                style { color(Styles.colors.red) }
            }) { Text(" ♥ ") }
            Span { appText { inHCMC } }
        }
    }
}
