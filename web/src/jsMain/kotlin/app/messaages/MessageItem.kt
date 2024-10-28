package app.messaages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.deleteMessage
import app.ailaai.api.updateMessage
import app.dialog.dialog
import app.dialog.inputDialog
import app.menu.Menu
import appString
import application
import com.queatz.db.Bot
import com.queatz.db.MemberAndPerson
import com.queatz.db.Message
import components.IconButton
import components.ProfilePhoto
import kotlinx.browser.window
import kotlinx.coroutines.launch
import notEmpty
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r

@Composable
fun MessageItem(
    message: Message,
    previousMessage: Message?,
    member: MemberAndPerson?,
    bot: Bot?,
    myMember: MemberAndPerson?,
    bots: List<Bot>,
    canReply: Boolean,
    onReply: () -> Unit,
    onReplyInNewGroup: () -> Unit,
    onUpdated: () -> Unit
) {
    val scope = rememberCoroutineScope()
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

        var showOptionsMenuButton by remember { mutableStateOf(false) }
        var messageMenuTarget by remember { mutableStateOf<DOMRect?>(null) }

        if (messageMenuTarget != null) {
            Menu({ messageMenuTarget = null }, messageMenuTarget!!) {
                if (canReply) {
                    item(appString { reply }) {
                        onReply()
                    }
                }

                item(appString { replyInNewGroup }) {
                    onReplyInNewGroup()
                }

                if (message.member == myMember?.member?.id) {
                    item(appString { edit }) {
                        scope.launch {
                            inputDialog(
                                // todo: translate
                                title = "Edit message",
                                confirmButton = application.appString { update },
                                singleLine = false,
                                defaultValue = message.text.orEmpty()
                            ).let {
                                if (it != null) {
                                    api.updateMessage(id = message.id!!, messageUpdate = Message(text = it)) {
                                        onUpdated()
                                    }
                                }
                            }
                        }
                    }
                }

                if (message.member == myMember?.member?.id || myMember?.member?.host == true) {
                    item(appString { delete }) {
                        scope.launch {
                            dialog(
                                // todo: translate
                                title = "Delete this message?",
                                confirmButton = application.appString { delete }
                            ).let {
                                if (it == true) {
                                    api.deleteMessage(message = message.id!!) {
                                        onUpdated()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Div({
            style {
                width(100.percent)
                gap(.5.r)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.FlexEnd)
                if (!isMe) {
                    flexDirection(FlexDirection.RowReverse)
                }
            }

            onMouseEnter { showOptionsMenuButton = true }
            onMouseLeave { showOptionsMenuButton = false }
        }) {
            var showingSticker by remember { mutableStateOf(false) }
            if (!showingSticker) {
                // todo: translate
                IconButton("more_vert", "Options", background = true, styles = {
                    opacity(if (showOptionsMenuButton || messageMenuTarget != null) 1f else 0f)
                }) {
                    messageMenuTarget =
                        if (messageMenuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
                }
            }
            MessageContent(message, myMember) {
                showingSticker = it
            }
        }
        message.bots?.notEmpty?.let {
            MessageBots(bots, it, isMine = isMe)
        }
    }
}
