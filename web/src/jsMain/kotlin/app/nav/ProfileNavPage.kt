package app.nav

import LocalConfiguration
import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.platformMe
import app.ailaai.api.profile
import app.ailaai.api.sendAppFeedback
import app.ailaai.api.transferCode
import app.ailaai.api.updateMe
import app.ailaai.api.updateProfile
import app.components.EditField
import app.dialog.dialog
import app.dialog.inputDialog
import appString
import appText
import application
import com.queatz.db.AppFeedback
import com.queatz.db.AppFeedbackType
import com.queatz.db.Person
import com.queatz.db.PersonProfile
import com.queatz.db.Profile
import components.IconButton
import components.QrImg
import components.Wbr
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import webBaseUrl
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProfileNavPage(
    onProfileClick: () -> Unit,
    onPlatformClick: () -> Unit,
    onScriptsClick: () -> Unit,
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var profile by remember {
        mutableStateOf<PersonProfile?>(null)
    }

    var isPlatformHost by remember { mutableStateOf(false) }
    var sendAppFeedback by remember { mutableStateOf<AppFeedbackType?>(null) }

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

    LaunchedEffect(Unit) {
        api.platformMe {
            isPlatformHost = it.host
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
        IconButton(
            name = "qr_code",
            title = appString { qrCode },
            styles = { marginRight(.5f.r) }
        ) {
            scope.launch {
                dialog("", cancelButton = null) {
                    QrImg("$webBaseUrl/profile/${me!!.id!!}")
                }
            }
        }
    }

    if (sendAppFeedback != null) {
        scope.launch {
            val title = when (sendAppFeedback) {
                // todo: translate
                AppFeedbackType.Suggestion -> "Request a feature"
                // todo: translate
                AppFeedbackType.Issue -> "Report a bug"
                // todo: translate
                AppFeedbackType.Other -> "Other feedback"
                else -> ""
            }

            val feedback = inputDialog(
                title = title,
                // todo: translate
                confirmButton = "Send"
            )

            if (feedback != null) {
                var success = false
                api.sendAppFeedback(AppFeedback(feedback = feedback, type = sendAppFeedback!!)) {
                    success = true
                    sendAppFeedback = null
                }

                if (success) {
                    // todo: translate
                    dialog("Thank you!", cancelButton = null)
                } else {
                    val failMessage = when (sendAppFeedback) {
                        // todo: translate
                        AppFeedbackType.Suggestion -> "Failed to send feature request"
                        // todo: translate
                        AppFeedbackType.Issue -> "Failed to send bug report"
                        // todo: translate
                        AppFeedbackType.Other -> "Failed to send feedback"
                        else -> ""
                    }
                    dialog(failMessage, cancelButton = null)
                    sendAppFeedback = null
                }
            } else {
                sendAppFeedback = null
            }
        }
    }

    NavMenu {
        val yourName = appString { yourName }
        val update = appString { update }
        val yourUrl = appString { yourProfileUrl }

        NavMenuItem("account_circle", me?.name?.notBlank ?: yourName) {
            scope.launch {
                val name = inputDialog(
                    title = yourName,
                    confirmButton = update,
                    defaultValue = me?.name ?: ""
                )

                api.updateMe(Person(name = name)) {
                    application.setMe(it)
                }
            }
        }

        val url = profile?.profile?.url?.notBlank
        val link = url?.let { "$webBaseUrl/$it" } ?: "$webBaseUrl/profile/${me?.id!!}"

        NavMenuItem("link", link) {
            scope.launch {
                val name = inputDialog(
                    title = yourUrl,
                    confirmButton = update,
                    defaultValue = profile?.profile?.url?.notBlank.orEmpty()
                )

                api.updateProfile(Profile(url = name)) {
                    reload()
                }
            }
        }

        var copiedLink by remember { mutableStateOf(false) }

        Div(
            {
                style {
                    display(DisplayStyle.Flex)
                    gap(.5f.r)
                    padding(.5f.r)
                }
            }
        ) {
            Button(
                {
                    classes(Styles.button)

                    style {
                        flex(1)
                        textAlign("center")
                        justifyContent(JustifyContent.Center)
                    }

                    onClick {
                        scope.launch {
                            val clipboardText = link
                            window.navigator.clipboard.writeText(clipboardText)
                            copiedLink = true
                            delay(2.seconds)
                            copiedLink = false
                        }
                    }
                }
            ) {
                Text(
                    if (copiedLink) {
                        appString { copied }
                    } else {
                        appString { copyProfileLink }
                    }
                )
            }
            Button(
                {
                    classes(Styles.outlineButton)

                    style {
                        flex(1)
                        textAlign("center")
                        justifyContent(JustifyContent.Center)
                    }

                    onClick {
                        window.open(link, "_blank")
                    }
                }
            ) {
                appText { openProfile }
            }
        }

        if (profile != null) {
            EditField(
                profile?.profile?.about ?: "",
                placeholder = appString { introduceYourself },
                styles = {
                    margin(.5.r)
                }
            ) {
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
                    "vi" -> "ru"
                    else -> "en"
                }
            )
        }

        val signOut = appString { signOut }
        val signOutOrTransfer = appString { signOutOrTransfer }
        val signOutQuestion = appString { signOutQuestion }
        val showTransferCode = appString { showTransferCode }
        var transferCode by remember { mutableStateOf("") }
        var showingTransferCode by remember { mutableStateOf(false) }

        fun loadTransferCode() {
            scope.launch {
                api.transferCode {
                    transferCode = it.code ?: ""
                    showingTransferCode = true
                }
            }
        }

        NavMenuItem("history_edu", appString { scripts }) {
            onScriptsClick()
        }

        if (isPlatformHost) {
            NavMenuItem("guardian", appString { platform }) {
                onPlatformClick()
            }
        }

        // todo: translate
        NavMenuItem("lightbulb", "Request a feature", iconColor = Styles.colors.green) {
            sendAppFeedback = AppFeedbackType.Suggestion
        }

        // todo: translate
        NavMenuItem("bug_report", "Report a bug", iconColor = Styles.colors.red) {
            sendAppFeedback = AppFeedbackType.Issue
        }

        // todo: translate
        NavMenuItem("feedback", "Other feedback", iconColor = Styles.colors.primary) {
            sendAppFeedback = AppFeedbackType.Other
        }

        NavMenuItem("logout", signOutOrTransfer) {
            scope.launch {
                val result = dialog(
                    title = signOutQuestion,
                    confirmButton = signOut
                ) {
                    if (showingTransferCode) {
                        appText { yourTransferCodeIs }
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                gap(0.5.r)
                                padding(0.5.r)
                                margin(0.5.r)
                                justifyContent(JustifyContent.Center)
                            }
                        }) {
                            Div({
                                style {
                                    padding(1.r)
                                    textAlign("center")
                                }
                            }) {
                                Text(transferCode)
                            }
                        }
                        appText { signOutWarning }
                    } else {
                        appText { signOutDescription }
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                gap(0.5.r)
                                padding(0.5.r)
                                margin(0.5.r)
                                justifyContent(JustifyContent.Center)
                            }
                        }) {
                            Button({
                                classes(Styles.button)
                                onClick {
                                    loadTransferCode()
                                }
                            }) {
                                Text(showTransferCode)
                            }
                        }
                        Wbr()
                        Text(" ")
                        appText { signOutQuestionLine1 }
                        Wbr()
                        Text(" ")
                        appText { signOutQuestionLine2 }
                    }
                }

                if (result == true) {
                    application.signOut()
                    window.location.pathname = "/"
                    window.location.reload()
                }

                showingTransferCode = false
                transferCode = ""
            }
        }
    }
}
