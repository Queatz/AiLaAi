package components

import Styles
import androidx.compose.runtime.*
import api
import app.ailaai.api.me
import app.ailaai.api.signIn
import app.ailaai.api.signInWithLink
import app.ailaai.api.signUp
import app.softwork.routingcompose.Router
import appString
import appText
import application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import linkDevice
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import qr
import r
import webBaseUrl
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun SigninPage() {
    val scope = rememberCoroutineScope()
    val router = Router.current

    var qrCode by remember {
        mutableStateOf<String?>(null)
    }
    var status by remember {
        mutableStateOf<String?>(null)
    }
    var qrCodeLinked by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        var linkDeviceToken: String? = null

        api.linkDevice {
            linkDeviceToken = it.token!!
        }

        if (linkDeviceToken == null) {
            status = application.appString {
                this.error
            }
            return@LaunchedEffect
        }

        qrCode = "$webBaseUrl/link-device/$linkDeviceToken".qr

        withTimeoutOrNull(5.minutes) {
            while (!qrCodeLinked) {
                delay(2.seconds)
                api.linkDevice(linkDeviceToken!!) {
                    if (it.person != null) {
                        qrCodeLinked = true
                    }
                }
            }
        }
        if (qrCodeLinked) {
            api.signInWithLink(
                linkDeviceToken!!,
                onError = {
                    status = application.appString { error }
                }
            ) {
                application.setToken(it.token)

                api.me {
                    application.setMe(it)
                    status = "Xin chào, ${it.name}"
                    router.navigate("/")
                }
            }
        }
    }

    fun signIn(transferCode: String) {
        scope.launch {
            api.signIn(
                transferCode = transferCode,
                onError = {
                    status = application.appString { error }
                }
                ) {
                application.setToken(it.token)

                api.me {
                    application.setMe(it)
                    status = "Xin chào, ${it.name}"
                    router.navigate("/")
                }
            }
        }
    }

    fun signUp(inviteCode: String? = null) {
        scope.launch {
            api.signUp(
                inviteCode,
                onError = {
                    status = application.appString { error }

                }
            ) {
                application.setToken(it.token)

                api.me {
                    application.setMe(it)
                }

                status = "Xin chào!"
                router.navigate("/")
            }
        }
    }

    Div({
        classes(Styles.mainContent)
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
                    alignItems(AlignItems.Center)
                }
            }) {
                if (!qrCodeLinked && qrCode != null) {
                    Img(src = qrCode!!)
                }

                Div({
                    style {
                        color(Styles.colors.secondary)
                        padding(1.r)
                    }
                }) {
                    Text(status ?: appString { scanQrCode })
                }

                val orEnterTransferCode = appString { orEnterTransferCode }

                Input(InputType.Text) {
                    classes(Styles.textarea)
                    style {
                        width(100.percent)
                        maxWidth(36.r)
                    }

                    placeholder(orEnterTransferCode)

                    onInput {
                        if(it.value.length == 16) {
                            signIn(it.value)
                            it.target.value = ""
                        }
                    }

                    autoFocus()
                }

                Div({
                    style {
                        color(Styles.colors.primary)
                        fontWeight("bold")
                        margin(1.r)
                        cursor("pointer")
                    }

                    onClick {
                        signUp()
                    }
                }) {
                    appText { signUp }
                }
            }
        }
    }
}
