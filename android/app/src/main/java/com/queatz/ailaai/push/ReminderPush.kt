package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.ReminderPushData

fun Push.receive(data: ReminderPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/reminders".toUri(),
        context,
        MainActivity::class.java
    )

    if (data.show != false) {
        send(
            deeplinkIntent,
            Notifications.Reminders,
            groupKey = "reminders/${data.reminder.id}",
            title = data.reminder.title ?: "",
            text = data.occurrence?.note ?: data.reminder.note ?: ""
        )
    }
}
