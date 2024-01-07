package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.attachmentText
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.nullIfBlank
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.MessagePushData
import kotlinx.coroutines.launch

fun Push.receive(data: MessagePushData) {
    if (
        latestEvent == Lifecycle.Event.ON_RESUME &&
        navController?.currentBackStackEntry?.destination?.route == "group/{id}" &&
        navController?.currentBackStackEntry?.arguments?.getString("id") == data.group.id
    ) {
        scope.launch {
            latestMessageFlow.emit(data.group.id)
        }
        return
    }

    // Don't notify notifications from myself, but do update latestMessage flow
    if (data.person.id == meId) {
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
            title = groupName ?: personName,
            text = buildString {
                if (groupName != null) {
                    append("$personName: ")
                }
                append(data.message.text?.nullIfBlank ?: data.message.attachmentText(context) ?: "")
            }
        )
    }
}
