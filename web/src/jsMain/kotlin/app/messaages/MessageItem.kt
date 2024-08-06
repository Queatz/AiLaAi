package app.messaages

import androidx.compose.runtime.Composable
import app.AppStyles
import com.queatz.db.Bot
import com.queatz.db.MemberAndPerson
import com.queatz.db.Message
import components.ProfilePhoto
import kotlinx.browser.window
import notEmpty
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun MessageItem(
    message: Message,
    previousMessage: Message?,
    member: MemberAndPerson?,
    bot: Bot?,
    myMember: MemberAndPerson?,
    bots: List<Bot>
) {
    val isMe = message.member == myMember?.member?.id

    Div({
        classes(
            listOf(AppStyles.messageLayout) + if (isMe) {
                listOf(AppStyles.myMessageLayout)
            } else {
                emptyList()
            }
        )
    }) {
        if (!isMe) {
            if (member?.member?.id == previousMessage?.member && bot?.id == previousMessage?.bot) {
                Div({
                    style {
                        width(36.px)
                        height(36.px)
                        marginRight(.5.r)
                        flexShrink(0)
                    }
                })
            } else {
                when {
                    message.member != null && member?.person != null -> {
                        ProfilePhoto(member.person!!, onClick = {
                            window.open("/profile/${member.person!!.id!!}")
                        }) {
                            marginRight(.5.r)
                        }
                    }
                    message.bot != null -> {
                        ProfilePhoto(bot?.photo, bot?.name) {
                            marginRight(.5.r)
                        }
                    }
                }

            }
        }
        MessageContent(message, myMember)
        message.bots?.notEmpty?.let {
            MessageBots(bots, it, isMine = isMe)
        }
    }
}
