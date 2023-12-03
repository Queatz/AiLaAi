package app.nav

import LocalConfiguration
import androidx.compose.runtime.*
import api
import app.ailaai.api.profile
import app.ailaai.api.updateMe
import app.ailaai.api.updateProfile
import app.components.EditField
import app.dialog.dialog
import app.dialog.inputDialog
import appString
import appText
import application
import com.queatz.db.*
import components.IconButton
import components.Wbr
import kotlinx.browser.window
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import qr
import r
import webBaseUrl

@Composable
fun ProfileNavPage(onProfileClick: () -> Unit) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var profile by remember {
        mutableStateOf<PersonProfile?>(null)
    }

    suspend fun reload() {
        api.profile(me!!.id!!) {
            profile = it
        }
    }

    LaunchedEffect(me) {
        if (me != null) {
            reload()
        }
    }

    suspend fun saveAbout(value: String): Boolean {
        var success = false
        api.updateProfile(Profile(about = value)) {
            success = true
            reload()
        }

        return success
    }

    NavTopBar(me, appString { this.profile }, onProfileClick = onProfileClick) {
        IconButton("qr_code", appString { qrCode }, styles = {
        }) {
            scope.launch {
                dialog("", cancelButton = null) {
                    val qrCode = remember {
                        "$webBaseUrl/profile/${me!!.id!!}".qr
                    }
                    Img(src = qrCode) {
                        style {
                            borderRadius(1.r)
                        }
                    }
                }
            }
        }

        IconButton("open_in_new", appString { viewProfile }, styles = {
            marginRight(.5.r)
        }) {
            window.open("/profile/${me!!.id!!}", "_blank")
        }
    }

    Div({
        style {
            overflowY("auto")
            overflowX("hidden")
            padding(1.r / 2)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        val yourName = appString { yourName }
        val update = appString { update }

        NavMenuItem("account_circle", me?.name?.notBlank ?: yourName) {
            scope.launch {
                val name = inputDialog(
                    yourName,
                    confirmButton = update,
                    defaultValue = me?.name ?: ""
                )

                api.updateMe(Person(name = name)) {
                    application.setMe(it)
                }
            }
        }

        if (profile != null) {
            EditField(profile?.profile?.about ?: "", placeholder = appString { introduceYourself }, styles = {
                margin(.5.r)
            }) {
                saveAbout(it)
            }
        }

        val configuration = LocalConfiguration.current

        NavMenuItem(
            when (configuration.language) {
                "vi" -> "\uD83C\uDDFB\uD83C\uDDF3"
                "ru" -> "\uD83C\uDDF7\uD83C\uDDFA"
                else -> "\uD83C\uDDEC\uD83C\uDDE7"
            },
            when (configuration.language) {
                "vi" -> "Language"
                "ru" -> "Язык"
                else -> "Ngôn ngữ"
            },
            textIcon = true
        ) {
            configuration.set(
                when (configuration.language) {
                    "en" -> "vi"
                    //"vi" -> "ru"
                    else -> "en"
                }
            )
        }

        val signOut = appString { signOut }
        val signOutQuestion = appString { signOutQuestion }


        NavMenuItem("logout", signOut) {
            scope.launch {
                val result = dialog(signOutQuestion, signOut) {
                    appText { signOutQuestionLine1 }
                    Wbr()
                    Text(" ")
                    appText { signOutQuestionLine2 }
                }

                if (result == true) {
                    application.signOut()
                    window.location.pathname = "/"
                    window.location.reload()
                }
            }
        }
    }
}
