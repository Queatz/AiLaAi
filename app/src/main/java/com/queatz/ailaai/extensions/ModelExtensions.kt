package com.queatz.ailaai.extensions

import android.content.Context
import com.queatz.ailaai.*

data class ContactPhoto(
    val name: String = "",
    val photo: String? = null
)

fun GroupExtended.name(someone: String, emptyGroup: String, omit: List<String>) =
    group?.name?.nullIfBlank
        ?: members
            ?.filter { !omit.contains(it.person?.id) }
            ?.mapNotNull { it.person }
            ?.joinToString { it.name ?: someone }
            ?.nullIfBlank
        ?: emptyGroup

fun GroupExtended.photos(omit: List<Person> = emptyList()) = members
    ?.filter {
        omit.none { person -> it.person?.id == person.id }
    }
    ?.map {
        ContactPhoto(it.person?.name ?: "", it.person?.photo)
    } ?: listOf(ContactPhoto())

fun GroupExtended.isUnread(member: Member?): Boolean {
    return (member?.seen?.toEpochMilliseconds() ?: return false) < (latestMessage?.createdAt?.toEpochMilliseconds() ?: return false)
}

fun Message.attachmentText(context: Context): String? = when (val attachment = getAttachment()) {
    is CardAttachment -> {
        if (attachment.card != null) {
            context.getString(R.string.sent_a_card)
        } else {
            null
        }
    }
    is PhotosAttachment -> {
        context.resources.getQuantityString(R.plurals.sent_photos, attachment.photos?.size ?: 0, attachment.photos?.size ?: 0)
    }
    else -> null
}
