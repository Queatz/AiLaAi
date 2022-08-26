package com.queatz.ailaai

import com.google.gson.JsonElement

class CardAttachment(
    var card: String? = null
) : MessageAttachment() {
    override val type = "card"
}

abstract class MessageAttachment  {
    abstract val type: String
}

fun Message.getAttachment() = attachment?.let {
    val jsonElement = gson.fromJson(it, JsonElement::class.java).asJsonObject

    when (jsonElement.getAsJsonPrimitive("type")?.asString) {
        "card" -> gson.fromJson(jsonElement, CardAttachment::class.java)
        else -> null
    }
}
