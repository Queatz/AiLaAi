package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.attachmentText
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.ailaai.services.isTopGroup
import com.queatz.push.MessageReactionPushData
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bulletedString
import kotlinx.coroutines.launch

fun Push.receive(data: MessageReactionPushData) {
    if (isTopGroup(data.group.id)) {
        // todo switch to push.events
        scope.launch {
            latestMessageFlow.emit(data.group.id)
        }
        return
    }

    // Don't notify notifications from myself, but do update latestMessage flow
    if (data.person?.id == meId) {
        return
    }

    // Don't need to show removed reactions
    if (data.react?.remove == true) {
        return
    }

    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/group/${data.group.id}".toUri(),
        context,
        MainActivity::class.java
    )

    if (data.show != false) {
        val groupName = data.group.name?.notBlank
        val personName = personNameOrYou(data.person)
        send(
            intent = deeplinkIntent,
            channel = Notifications.Messages,
            groupKey = makeGroupKey(data.group.id!!),
            replyInGroup = data.group.id!!,
            title = groupName ?: personName,
            text = buildString {
                if (groupName != null) {
                    append("$personName ")
                }
                append(
                    context.getString(
                        R.string.inline_reacted_x_to_x,
                        bulletedString(
                            data.react?.reaction?.reaction,
                            data.react?.reaction?.comment?.let { "\"$it\"" }
                        ),
                        (data.message?.text?.notBlank ?: data.message?.attachmentText(context))?.let { "\"$it\"" } ?: ""
                    )
                )
            }
        )
    }
}
