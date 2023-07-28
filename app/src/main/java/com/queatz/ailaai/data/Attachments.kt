package com.queatz.ailaai.data

import com.queatz.ailaai.extensions.inList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
class PhotosAttachment(
    var photos: List<String>? = null,
) : MessageAttachment() {
    override val type = "photos"
}

@Serializable
class AudioAttachment(
    var audio: String? = null,
) : MessageAttachment() {
    override val type = "audio"
}

@Serializable
class VideoAttachment(
    var video: String? = null,
) : MessageAttachment() {
    override val type = "video"
}

@Serializable
class CardAttachment(
    var card: String? = null,
) : MessageAttachment() {
    override val type = "card"
}

@Serializable
class ReplyAttachment(
    var message: String? = null,
) : MessageAttachment() {
    override val type = "reply"
}

@Serializable
class StoryAttachment(
    var story: String? = null,
) : MessageAttachment() {
    override val type = "story"
}

@Serializable
class StickerAttachment(
    var photo: String? = null,
    var sticker: String? = null,
    var message: String? = null,
) : MessageAttachment() {
    override val type = "sticker"
}

@Serializable
abstract class MessageAttachment {
    abstract val type: String
}

fun Message.getAttachment() = attachment?.asMessageAttachment()

fun Message.getAttachments() = attachments?.mapNotNull { it.asMessageAttachment() } ?: emptyList()

fun Message.getAllAttachments(): List<MessageAttachment> = getAttachment().inList() + getAttachments()

private fun String.asMessageAttachment(): MessageAttachment? {
    return try {
        val jsonElement = json.decodeFromString<JsonElement>(this)
        when (jsonElement.jsonObject["type"]?.jsonPrimitive?.contentOrNull) {
            "reply" -> json.decodeFromJsonElement<ReplyAttachment>(jsonElement)
            "card" -> json.decodeFromJsonElement<CardAttachment>(jsonElement)
            "photos" -> json.decodeFromJsonElement<PhotosAttachment>(jsonElement)
            "audio" -> json.decodeFromJsonElement<AudioAttachment>(jsonElement)
            "video" -> json.decodeFromJsonElement<VideoAttachment>(jsonElement)
            "story" -> json.decodeFromJsonElement<StoryAttachment>(jsonElement)
            "sticker" -> json.decodeFromJsonElement<StickerAttachment>(jsonElement)
            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
