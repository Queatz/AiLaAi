package app.invites

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.AppNavigation
import app.ailaai.api.me
import app.ailaai.api.profile
import app.ailaai.api.signUp
import app.ailaai.api.useInvite
import app.appNav
import app.dialog.dialog
import app.softwork.routingcompose.Router
import appString
import appText
import application
import com.queatz.db.Invite
import com.queatz.db.PersonProfile
import components.Loading
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import webBaseUrl

@Composable
fun InviteCard(
    invite: Invite
) {
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    val router = Router.current
    val appNav = appNav

    var inviter by remember { mutableStateOf<PersonProfile?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        api.profile(
            invite.person!!
        ) {
            inviter = it
        }
        isLoading = false
    }

    inviter?.let {
        Div(
            attrs = {
                style {
                    fontSize(24.px)
                    textAlign("center")
                    padding(1.r)
                }
            }
        ) {
            val url = it.profile.url?.notBlank
            val link = url?.let { "$webBaseUrl/$it" } ?: "$webBaseUrl/profile/${me?.id!!}"
            A(
                href = link,
                attrs = {
                    target(ATarget.Blank)

                    style {
                        fontWeight("bold")
                    }
                }
            ) {
                Text(it.person.name ?: appString { someone })
            }
            Text(" ${appString { isInvitingYouTo }} ")
            B {
                Text("${appString { appName }}!")
            }
        }
    }

    invite.about?.notBlank?.let { about ->
        Div(
            attrs = {
                style {
                    marginTop(1.r)
                    marginBottom(1.r)
                    padding(1.r)
                    border(1.px, LineStyle.Solid, Styles.colors.primary)
                    borderRadius(1.r)
                    property("word-break", "break-word")
                }
            }
        ) {
            Text(about)
        }
    }

    if (isLoading) {
        Loading()
    } else {
        Button(
            attrs = {
                classes(Styles.button)

                style {
                    marginTop(1.r)
                    justifyContent(JustifyContent.Center)
                    textAlign("center")
                }

                if (me?.id == invite.person) {
                    disabled()
                }

                onClick {
                    scope.launch {
                        if (me == null) {
                            api.signUp(
                                inviteCode = invite.code!!,
                                onError = { error ->
                                    scope.launch {
                                        dialog(
                                            title = application.appString { inviteCodeCannotBeUsed },
                                            cancelButton = null
                                        ) {
                                            (error as? ResponseException)?.response?.status?.description?.let {
                                                Text(it)
                                            }
                                        }
                                    }
                                }
                            ) {
                                application.setToken(it.token)

                                api.me {
                                    application.setMe(it)
                                }

                                router.navigate("/")
                            }
                        } else {
                            api.useInvite(
                                code = invite.code!!,
                                onError = {
                                    console.error(it)
                                    scope.launch {
                                        dialog(
                                            title = application.appString { inviteCodeCannotBeUsed },
                                            cancelButton = null
                                        )
                                    }
                                }
                            ) { response ->
                                response.group?.let { group ->
                                    appNav.navigate(
                                        destination = AppNavigation.Group(
                                            id = group
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) {
            appText { if (me == null) signUpAndAcceptInvite else acceptInvite }
        }
    }
}
