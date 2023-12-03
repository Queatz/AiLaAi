package app.messaages

import androidx.compose.runtime.Composable
import app.AppStyles
import com.queatz.db.MemberAndPerson
import com.queatz.db.Message
import components.ProfilePhoto
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun MessageItem(message: Message, previousMessage: Message?, member: MemberAndPerson?, myMember: MemberAndPerson?) {
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
        if (!isMe && member?.person != null) {
            if (member.member?.id == previousMessage?.member) {
                Div({
                    style {
                        width(36.px)
                        height(36.px)
                        marginRight(.5.r)
                        flexShrink(0)
                    }
                })
            } else {
                ProfilePhoto(member.person!!, onClick = {
                    window.open("/profile/${member.person!!.id!!}")
                }) {
                    marginRight(.5.r)
                }
            }
        }
        MessageContent(message, myMember)
    }
}
