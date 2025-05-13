package components

import LocalConfiguration
import Styles
import androidx.compose.runtime.*
import app.softwork.routingcompose.Router
import appString
import appText
import application
import ellipsize
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r

@Composable
fun AppHeader(
    title: String,
    showBack: Boolean = false,
    showMe: Boolean = true,
    background: Boolean = true,
    showDownloadApp: Boolean = false,
    onBack: () -> Unit = {},
) {
    val layout by application.layout.collectAsState()
    val router = Router.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    if (layout == AppLayout.Kiosk) {
        Div({
            style {
                height(1.r)
                width(100.percent)
            }
        }) {  }
        return
    }

    Div({
        classes(Styles.appHeader)

        if (!background) {
            style {
                margin(0.r)
                backgroundColor(Color.transparent)
                property("box-shadow", "none")
            }
        }
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
            title.notBlank?.let { title ->
                Span({
                    classes(Styles.desktopOnly)

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
            Text("")
        }
        if (showDownloadApp) {
            Button({
                classes(Styles.button)

                style {
                    marginRight(.5.r)
                }

                onClick {
                    scope.launch {
                        getAppDialog()
                    }
                }
            }) {
                appText { getTheApp }
            }
        }
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
                        "vi" -> "ru"
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
                    appText { signUp }
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
