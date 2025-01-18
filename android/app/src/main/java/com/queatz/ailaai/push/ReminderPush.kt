package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.PushNotificationAction
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.ReminderPushData

fun Push.receive(data: ReminderPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/schedule".toUri(),
        context,
        MainActivity::class.java
    )

    if (data.show != false) {
        send(
            intent = deeplinkIntent,
            channel = if (data.reminder.alarm == true) Notifications.Alarms else Notifications.Reminders,
            groupKey = "reminders/${data.reminder.id}",
            title = data.reminder.title.orEmpty(),
            text = data.occurrence?.note ?: data.reminder.note.orEmpty(),
            alarm = data.reminder.alarm == true,
            actions = PushNotificationAction.MarkAsDone(
                reminder = data.reminder.id!!,
                occurrence = data.occurrence?.occurrence ?: data.date
            ).inList()
        )
    }
}
