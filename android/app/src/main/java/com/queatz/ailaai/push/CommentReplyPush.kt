package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.CommentReplyPushData

fun Push.receive(data: CommentReplyPushData) {
    // Don't notify notifications from myself, allow content to refresh
    if (data.person?.id == meId) {
        return
    }

    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/story/${data.story!!.id!!}?comment=${data.onComment!!.id!!}".toUri(),
        context,
        MainActivity::class.java
    )

    send(
        deeplinkIntent,
        Notifications.Comments,
        groupKey = "comments/${data.comment!!.id!!}",
        title = context.getString(R.string.x_replied_to_your_comment, personNameOrYou(data.person, inline = false)),
        text = data.comment?.comment ?: ""
    )
}
