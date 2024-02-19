package com.queatz.ailaai.push

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.os.bundleOf
import com.queatz.ailaai.CallActivity
import com.queatz.ailaai.CallActivity.Companion.GROUP_ID_EXTRA
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.CallPushData


fun Push.receive(data: CallPushData) {
    // todo fancy call push (Dismiss) (Join)
    // todo ongoing call notification

    val deeplinkIntent = Intent(
        context,
        CallActivity::class.java
    ).apply {
        action = Intent.ACTION_CALL
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtras(
            bundleOf(
                GROUP_ID_EXTRA to data.group.id!!
            )
        )
    }

    if (data.show != false) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) ?:
            Uri.parse("android.resource://${context.packageName}/${R.raw.call}")
        send(
            deeplinkIntent,
            Notifications.Calls,
            groupKey = "calls/${data.group.id!!}",
            title = data.group.name?.notBlank ?: data.person.name?.notBlank ?: context.getString(R.string.someone),
            text = context.getString(R.string.tap_to_answer),
            sound = soundUri
        )
    }
}
