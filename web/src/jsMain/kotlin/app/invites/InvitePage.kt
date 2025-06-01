package app.invites

import StyleManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyleSheet
import app.FullPageLayout
import app.ailaai.api.invite
import app.components.Empty
import appText
import com.queatz.db.Invite
import components.Loading
import mainContent
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun InvitePage(code: String) {
    StyleManager.use(
        AppStyleSheet::class
    )

    var isLoading by remember {
        mutableStateOf(true)
    }
    var isError by remember {
        mutableStateOf(false)
    }
    var invite by remember {
        mutableStateOf<Invite?>(null)
    }

    LaunchedEffect(code) {
        api.invite(
            code = code,
            onError = {
                isError = true
            }
        ) {
            invite = it
        }
        isLoading = false
    }

    Div({
        mainContent()
    }) {
        if (isError) {
            Empty {
                appText { inviteNotFound }
            }
        } else if (isLoading) {
            Loading()
        } else if (invite != null) {
            FullPageLayout {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(0.r, 1.r, 1.r, 1.r)
                    }
                }) {
                    InviteCard(invite!!)
                }
            }
        }
    }
}
