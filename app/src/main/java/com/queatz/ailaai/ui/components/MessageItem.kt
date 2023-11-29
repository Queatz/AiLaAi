package com.queatz.ailaai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Message
import com.queatz.db.Person

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    previousMessage: Message?,
    selectedMessages: Set<Message>,
    onSelectedChange: (Message, Boolean) -> Unit,
    getPerson: (String) -> Person?,
    getMessage: suspend (String) -> Message?,
    me: String?,
    onDeleted: () -> Unit,
    onReply: (Message) -> Unit,
    onShowPhoto: (String) -> Unit
) {
    var showTime by rememberStateOf(false)
    var showMessageDialog by rememberStateOf(false)
    val isMe = me == message.member
    val nav = nav

    Row(modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = {
                showTime = !showTime
            },
            onLongClick = {
                showMessageDialog = true
            }
        )) {
        if (!isMe) {
            if (previousMessage?.member != message.member) {
                ProfileImage(
                    getPerson(message.member!!),
                    PaddingValues(1.pad, 1.pad, 0.dp, 1.pad),
                ) { person ->
                    nav.navigate("profile/${person.id!!}")
                }
            } else {
                Box(Modifier.requiredSize(32.dp + 1.pad))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            MessageContent(
                message,
                isMe,
                me,
                showTime,
                {
                    if (selectedMessages.isEmpty()) {
                        showTime = it
                    } else {
                        onSelectedChange(message, message !in selectedMessages)
                    }
                },
                showMessageDialog,
                { showMessageDialog = it },
                getPerson,
                getMessage,
                onReply,
                onDeleted,
                onShowPhoto,
                selected = message in selectedMessages,
                onSelectedChange = {
                    onSelectedChange(message, it)
                }
            )
        }
    }
}
