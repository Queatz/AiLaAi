package com.queatz.ailaai.extensions

import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.api.cardGroup
import com.queatz.ailaai.api.sendMessage
import com.queatz.ailaai.data.*
import kotlinx.serialization.encodeToString

val Card.url get() = "$appDomain/card/$id"

fun cardUrl(id: String) = "$appDomain/card/$id"
fun storyUrl(urlOrId: String) = "$appDomain/story/$urlOrId"
fun profileUrl(id: String) = "$appDomain/profile/$id"

suspend fun Card.reply(conversation: List<String>, onSuccess: (groupId: String) -> Unit = {}) {
    api.cardGroup(id!!) { group ->
        api.sendMessage(
            group.id!!,
            Message(
                text = conversation.filterNotBlank().ifNotEmpty?.joinToString(" → "),
                attachment = json.encodeToString(CardAttachment(id!!))
            )
        ) {
            onSuccess(group.id!!)
        }
    }
}

val Card.latLng get() = geo?.let { LatLng(it[0], it[1]) }
