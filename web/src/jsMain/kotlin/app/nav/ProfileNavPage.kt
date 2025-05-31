package app.nav

import LocalConfiguration
import Styles
import account
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
import app.components.FlexInput
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
import components.Loading
import components.QrImg
import components.Wbr
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import linkDevice
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import qr
import r
import webBaseUrl
import kotlin.time.Duration.Companion.seconds

/**
 * Shows a dialog for sending app feedback.
 *
 * @param feedbackType The type of feedback (suggestion, issue, other)
 * @return True if feedback was successfully sent, false otherwise
 */
suspend fun appFeedbackDialog(
    feedbackType: AppFeedbackType,
    prefix: String = ""
): Boolean {
    val title = when (feedbackType) {
        // todo: translate
        AppFeedbackType.Suggestion -> "Request a feature"
        // todo: translate
        AppFeedbackType.Issue -> "Report a bug"
        // todo: translate
        AppFeedbackType.Other -> "Other feedback"
    }

    val feedback = inputDialog(
        title = title,
        // todo: translate
        confirmButton = "Send",
        singleLine = false
    )

    if (!feedback.isNullOrBlank()) {
        var success = false
        api.sendAppFeedback(AppFeedback(feedback = "$prefix$feedback", type = feedbackType)) {
            success = true
        }

        if (success) {
            // todo: translate
            dialog("Thank you!", cancelButton = null)
        } else {
            val failMessage = when (feedbackType) {
                // todo: translate
                AppFeedbackType.Suggestion -> "Failed to send feature request"
                // todo: translate
                AppFeedbackType.Issue -> "Failed to send bug report"
                // todo: translate
                AppFeedbackType.Other -> "Failed to send feedback"
            }
            dialog(failMessage, cancelButton = null)
        }

        return success
    }

    return false
}

@Composable
fun ProfileNavPage(
    onProfileClick: () -> Unit,
    onPlatformClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onScenesClick: () -> Unit = {},
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var profile by remember {
        mutableStateOf<PersonProfile?>(null)
    }

    var isPlatformHost by remember { mutableStateOf(false) }
    var sendAppFeedback by remember { mutableStateOf<AppFeedbackType?>(null) }
    var points by remember { mutableStateOf<Int?>(null) }
    var showPointsDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        api.account {
            points = it.points ?: 0
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
                dialog(
                    title = application.appString { myQr },
                    cancelButton = null
                ) {
                    QrImg("$webBaseUrl/profile/${me!!.id!!}")
                }
            }
        }
    }

    if (showPointsDialog) {
        scope.launch {
            // todo: translate
            dialog("Points", cancelButton = null, confirmButton = "Close") {
                // todo: translate
                Text("Points are earned by contributing to the community. You can use points to access premium features.")
            }
            showPointsDialog = false
        }
    }

    if (sendAppFeedback != null) {
        scope.launch {
            appFeedbackDialog(sendAppFeedback!!)
            sendAppFeedback = null
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

        // todo: translate
        NavMenuItem("stars", if (points == null) "Loading points..." else "$points points") {
            showPointsDialog = true
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
            val profileAbout = profile?.profile?.about.orEmpty()
            var messageText by remember(profileAbout) { mutableStateOf(profileAbout) }

            Div({
                style {
                    margin(.5.r)
                }
            }) {
                FlexInput(
                    value = messageText,
                    onChange = { newText ->
                        messageText = newText
                    },
                    placeholder = appString { introduceYourself },
                    showButtons = true,
                    buttonText = appString { save },
                    onSubmit = {
                        saveAbout(messageText)
                    }
                )
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

        // Link Device button
        // todo: translate
        NavMenuItem("phone_android", "Link device") {
            scope.launch {
                dialog(
                    title = "Link device",
                    cancelButton = null
                ) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            alignItems(AlignItems.Center)
                            gap(1.r)
                            padding(1.r)
                        }
                    }) {
                        // todo: translate
                        Text("Scan this QR code from your other device to sign in")

                        var qrCode by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(Unit) {
                            api.linkDevice {
                                qrCode = "$webBaseUrl/link-device/${it.token!!}".qr
                            }
                        }

                        Div({
                            style {
                                borderRadius(1.r)
                                overflow("hidden")
                            }
                        }) {
                            if (qrCode != null) {
                                Img(src = qrCode!!)
                            } else {
                                // todo: translate
                                Loading()
                            }
                        }
                    }
                }
            }
        }


        if (isPlatformHost) {
            NavMenuItem("guardian", appString { platform }) {
                onPlatformClick()
            }
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
                            }
                        }) {
                            Button({
                                classes(Styles.outlineButton)
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
