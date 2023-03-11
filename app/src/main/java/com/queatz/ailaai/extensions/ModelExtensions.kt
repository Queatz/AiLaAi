package com.queatz.ailaai.extensions

import android.content.Context
import com.queatz.ailaai.*

fun GroupExtended.name(someone: String, omit: List<String>) =
    group?.name?.nullIfBlank
        ?: members
            ?.filter { !omit.contains(it.person?.id) }
            ?.mapNotNull { it.person }
            ?.joinToString { it.name ?: someone }
        ?: ""

fun GroupExtended.photos(omit: List<Person> = emptyList()) = members
    ?.filter {
        omit.none { person -> it.person?.id == person.id }
    }
    ?.map {
        it.person?.photo ?: ""
    } ?: listOf("")

fun Message.attachmentText(context: Context): String? = when (val attachment = getAttachment()) {
    is CardAttachment -> {
        if (attachment.card != null) {
            context.getString(R.string.sent_a_card)
        } else {
            null
        }
    }
    else -> null
}
