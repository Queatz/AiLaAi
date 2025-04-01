package app.invites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import api
import app.AppNavigation
import app.ailaai.api.me
import app.ailaai.api.signUp
import app.ailaai.api.useInvite
import app.appNav
import app.softwork.routingcompose.Router
import application
import com.queatz.db.Invite
import com.queatz.db.StoryContent.Text
import components.Loading
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Button

@Composable
fun InviteCard(invite: Invite) {
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    val router = Router.current
    val appNav = appNav

    var isLoading by remember { mutableStateOf(false) }

    Text(invite.about.orEmpty())

    if (isLoading) {
        Loading()
    } else {
        Button(
            attrs = {
                onClick {
                    scope.launch {
                        if (me == null) {
                            api.signUp(
                                inviteCode = invite.code!!
                            ) {
                                application.setToken(it.token)

                                api.me {
                                    application.setMe(it)
                                }

                                router.navigate("/")
                            }
                        } else {
                            api.useInvite(
                                code = invite.code!!
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
            // todo: translate
            Text("Accept invite")
        }
    }
}
