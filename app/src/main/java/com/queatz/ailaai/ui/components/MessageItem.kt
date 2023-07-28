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
import androidx.navigation.NavController
import com.queatz.ailaai.data.Message
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    previousMessage: Message?,
    getPerson: (String) -> Person?,
    getMessage: suspend (String) -> Message?,
    me: String?,
    onDeleted: () -> Unit,
    onReply: (Message) -> Unit,
    onShowPhoto: (String) -> Unit,
    navController: NavController,
) {
    var showTime by rememberStateOf(false)
    var showMessageDialog by rememberStateOf(false)
    val isMe = me == message.member

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
                    PaddingValues(PaddingDefault, PaddingDefault, 0.dp, PaddingDefault),
                ) { person ->
                    navController.navigate("profile/${person.id!!}")
                }
            } else {
                Box(Modifier.requiredSize(32.dp + PaddingDefault))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            MessageContent(
                message,
                isMe,
                me,
                showTime,
                { showTime = it },
                showMessageDialog,
                { showMessageDialog = it },
                getPerson,
                getMessage,
                onReply,
                onDeleted,
                onShowPhoto,
                navController
            )
        }
    }
}
