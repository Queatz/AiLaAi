package com.queatz.ailaai.extensions

import app.ailaai.api.cardGroup
import app.ailaai.api.sendMessage
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.json
import com.queatz.db.Card
import com.queatz.db.CardAttachment
import com.queatz.db.Message
import kotlinx.serialization.encodeToString

val Card.url get() = "$appDomain/page/$id"

fun cardUrl(id: String) = "$appDomain/page/$id"
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

val Card.latLng get() = geo!!.let { LatLng(it[0], it[1]) }
