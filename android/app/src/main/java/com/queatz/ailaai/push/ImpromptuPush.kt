package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.formatDistance
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.db.ImpromptuNotificationStyle
import com.queatz.push.ImpromptuPushData

fun Push.receive(
    data: ImpromptuPushData
) {
    // Don't notify notifications from myself
    if (data.data?.otherPerson == meId) {
        return
    }

    val otherPersonId = data.data?.otherPerson ?: return
    val otherPersonName = data.data?.otherPersonDetails?.name ?: context.getString(R.string.someone)

    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/profile/$otherPersonId".toUri(),
        context,
        MainActivity::class.java
    )

    send(
        intent = deeplinkIntent,
        channel = Notifications.Impromptu,
        passive = data.notificationType == ImpromptuNotificationStyle.Passive,
        groupKey = makeGroupKey(otherPersonId),
        title = otherPersonName,
        // todo: translate
        text = context.getString(
            R.string.is_nearby_seeking,
            data.data?.distance?.formatDistance(context).orEmpty(),
            data.data?.seeksDetails?.firstOrNull()?.name
        )
    )
}
