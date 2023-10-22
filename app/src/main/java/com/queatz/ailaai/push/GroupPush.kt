package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.GroupEvent
import com.queatz.push.GroupPushData

fun Push.receive(data: GroupPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/group/${data.group.id}".toUri(),
        context,
        MainActivity::class.java
    )

    if (data.event == GroupEvent.Join) {
        send(
            deeplinkIntent,
            Notifications.Host,
            groupKey = "group/${data.group.id}",
            title = data.group.name ?: "",
            text = context.getString(
                R.string.x_added_x,
                personNameOrYou(data.details?.invitor),
                personNameOrYou(data.person)
            )
        )
    }
}
