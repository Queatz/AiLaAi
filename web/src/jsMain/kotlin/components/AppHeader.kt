package components

import LocalConfiguration
import Styles
import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import appString
import appText
import application
import ellipsize
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r

@Composable
fun AppHeader(
    title: String,
    showBack: Boolean = false,
    showMenu: Boolean = false,
    showMe: Boolean = true,
    onBack: () -> Unit = {},
) {
    val router = Router.current

    Div({
        classes(Styles.appHeader)
    }) {
        if (showBack) {
            Button({
                classes(Styles.textButton)
                style {
                    marginLeft(.5.r)
                }
                onClick {
                    onBack()
                }
            }) {
                Span({
                    classes("material-symbols-outlined")
                }) {
                    Text("arrow_back")
                }
                Text(" ${appString { goBack }}")
            }
        } else {
            Img("/icon.png") {
                style {
                    width(54.px)
                    height(54.px)
                    cursor("pointer")
                }
                onClick {
                    router.navigate("/")
                }
            }
            Span({
                style {
                    paddingLeft(1.r)
                    fontSize(24.px)
                    ellipsize()
                    marginRight(.5.r)
                }
            }) {
                Text(title)
            }
        }
        Div({
            style {
                width(0.px)
                flexGrow(1f)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.FlexEnd)
                overflow("hidden")
            }
        }) {
            if (showMenu) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        cursor("pointer")
                    }
                    onClick {
                        router.navigate("/cities")
                    }
                }) {
                    Span {
                        Text("Hồ Chí Minh")
                    }
                    Span({
                        classes("material-symbols-outlined")
                        style {
                        }
                    }) {
                        Text("expand_more")
                    }
                }
            } else {
                Text("")
            }
        }
        val configuration = LocalConfiguration.current
        Span({
            style {
                padding(.5.r, 1.r)
                fontSize(32.px)
                property("user-select", "none")
                cursor("pointer")
            }
            onClick {
                configuration.set(
                    when (configuration.language) {
                        "en" -> "vi"
                        //"vi" -> "ru"
                        else -> "en"
                    }
                )
            }
            title(
                when (configuration.language) {
                    "vi" -> "Language"
                    "ru" -> "Язык"
                    else -> "Ngôn ngữ"
                }
            )
        }) {
            when (configuration.language) {
                "vi" -> Text("\uD83C\uDDFB\uD83C\uDDF3")
                "ru" -> Text("\uD83C\uDDF7\uD83C\uDDFA")
                else -> Text("\uD83C\uDDEC\uD83C\uDDE7")
            }
        }
        if (showMe) {
            val me by application.me.collectAsState()

            if (me == null) {
                Button({
                    classes(Styles.textButton)
                    style {
                        marginRight(.5.r)
                    }
                    onClick {
                        router.navigate("/signin")
                    }
                }) {
                    appText { signIn }
                }
            } else {
                // todo show profile icon
            }
        }
    }
}

@Composable
fun Bullet() {
    Span({
        style {
            padding(0.r, .5.r)
            color(Styles.colors.primary)
            opacity(.75f)
        }
    }) {
        Text("•")
    }
}
