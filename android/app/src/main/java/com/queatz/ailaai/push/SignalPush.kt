package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.SignalPushData
import com.queatz.push.SignalReplyPushData

fun Push.receive(data: SignalPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/signals/${data.signalSend.id}".toUri(),
        context,
        MainActivity::class.java
    )

    val text = buildString {
        append("${data.signal.emoji} ${data.signal.name}")
        data.signalSend.message?.let { append(": $it") }
        data.transcription?.let { append("\n(Voice: $it)") }
    }

    send(
        intent = deeplinkIntent,
        channel = Notifications.Collaboration,
        groupKey = "signals",
        title = data.person.name ?: "Someone",
        text = text
    )
}

fun Push.receive(data: SignalReplyPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/signals/${data.signalSend.id}".toUri(),
        context,
        MainActivity::class.java
    )

    val text = buildString {
        append("Reply to ${data.signal.name}")
        data.signalReply.message?.let { append(": $it") }
        data.transcription?.let { append("\n(Voice: $it)") }
    }

    send(
        intent = deeplinkIntent,
        channel = Notifications.Collaboration,
        groupKey = "signals",
        title = data.person.name ?: "Someone",
        text = text
    )
}
