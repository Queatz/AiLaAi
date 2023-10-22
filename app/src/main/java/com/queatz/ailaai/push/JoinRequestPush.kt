package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.ailaai.services.joins
import com.queatz.push.JoinRequestPushData

fun Push.receive(data: JoinRequestPushData) {
    joins.onPush(data)

    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "${appDomain}/group/${data.group.id}".toUri(),
        context,
        MainActivity::class.java
    )

    send(
        deeplinkIntent,
        Notifications.Host,
        groupKey = "join-request/${data.joinRequest.id}",
        title = context.getString(R.string.x_requested_x, data.person.name ?: context.getString(R.string.someone), data.group.name ?: ""),
        text = data.joinRequest.message ?: ""
    )
}
