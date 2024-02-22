package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.schedule.asNaturalList
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.StoryPushData

fun Push.receive(data: StoryPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "$appDomain/story/${data.story.id}".toUri(),
        context,
        MainActivity::class.java
    )

    val title = data.story.title ?: return
    val authors = data.authors.map { personNameOrYou(it) }
    send(
        intent = deeplinkIntent,
        channel = Notifications.Subscriptions,
        groupKey = "story/${data.story.id!!}",
        title = title,
        text = context.getString(R.string.story_by_x, authors.asNaturalList(context) { it })
    )
}
