package com.queatz.ailaai

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

@Serializable
class PhotosAttachment(
    var photos: List<String>? = null
) : MessageAttachment() {
    override val type = "photos"
}

@Serializable
class CardAttachment(
    var card: String? = null
) : MessageAttachment() {
    override val type = "card"
}

@Serializable
abstract class MessageAttachment  {
    abstract val type: String
}

fun Message.getAttachment() = attachment?.let {
    val jsonElement = json.decodeFromString<JsonElement>(it)

    when (jsonElement.jsonObject["type"]?.jsonPrimitive?.contentOrNull) {
        "card" -> json.decodeFromJsonElement<CardAttachment>(jsonElement)
        "photos" -> json.decodeFromJsonElement<PhotosAttachment>(jsonElement)
        else -> null
    }
}
