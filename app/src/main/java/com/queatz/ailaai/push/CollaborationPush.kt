package com.queatz.ailaai.push

import android.content.Intent
import androidx.core.net.toUri
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.services.Notifications
import com.queatz.ailaai.services.Push
import com.queatz.push.CollaborationEvent
import com.queatz.push.CollaborationEventDataDetails
import com.queatz.push.CollaborationPushData

fun Push.receive(data: CollaborationPushData) {
    val deeplinkIntent = Intent(
        Intent.ACTION_VIEW,
        "${appDomain}/page/${data.card.id}".toUri(),
        context,
        MainActivity::class.java
    )

    send(
        deeplinkIntent,
        Notifications.Collaboration,
        groupKey = "collaboration/${data.card.id}",
        title = data.card.name ?: context.getString(R.string.collaboration),
        text = eventForCollaborationNotification(data)
    )
}

private fun Push.eventForCollaborationNotification(data: CollaborationPushData): String {
    val person = data.person.name ?: context.getString(R.string.someone)
    return when (data.event) {
        CollaborationEvent.AddedPerson -> context.getString(
            R.string.person_added_person,
            person,
            personNameOrYou(data.data.person)
        )

        CollaborationEvent.RemovedPerson -> context.getString(
            R.string.person_removed_person,
            person,
            personNameOrYou(data.data.person)
        )

        CollaborationEvent.AddedCard -> context.getString(
            R.string.person_added_card, person, data.data.card?.name ?: context.getString(
                R.string.inline_a_card
            )
        )

        CollaborationEvent.RemovedCard -> context.getString(
            R.string.person_removed_card, person, data.data.card?.name ?: context.getString(
                R.string.inline_a_card
            )
        )

        CollaborationEvent.UpdatedCard -> {
            if (data.data.card == null) {
                context.getString(R.string.person_updated_details, person, cardDetailName(data.data.details))
            } else {
                context.getString(
                    R.string.person_updated_card,
                    person,
                    cardDetailName(data.data.details),
                    data.data.card?.name ?: context.getString(
                        R.string.inline_a_card
                    )
                )
            }
        }
    }
}

private fun Push.cardDetailName(detail: CollaborationEventDataDetails?): String {
    return when (detail) {
        CollaborationEventDataDetails.Photo -> context.getString(R.string.inline_photo)
        CollaborationEventDataDetails.Video -> context.getString(R.string.inline_video)
        CollaborationEventDataDetails.Conversation -> context.getString(R.string.inline_group)
        CollaborationEventDataDetails.Name -> context.getString(R.string.inline_name)
        CollaborationEventDataDetails.Location -> context.getString(R.string.inline_hint)
        else -> ""
    }
}
