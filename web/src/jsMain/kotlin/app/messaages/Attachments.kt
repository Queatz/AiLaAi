package app.messaages

import androidx.compose.runtime.Composable
import com.queatz.db.*
import json
import kotlinx.serialization.json.*
import notBlank


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
            "videos" -> json.decodeFromJsonElement<VideosAttachment>(jsonElement)
            "story" -> json.decodeFromJsonElement<StoryAttachment>(jsonElement)
            "group" -> json.decodeFromJsonElement<GroupAttachment>(jsonElement)
            "sticker" -> json.decodeFromJsonElement<StickerAttachment>(jsonElement)
            "url" -> json.decodeFromJsonElement<UrlAttachment>(jsonElement)
            "trade" -> json.decodeFromJsonElement<TradeAttachment>(jsonElement)
            else -> null
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

fun <T> T?.inList() = this?.let(::listOf) ?: emptyList<T>()

@Composable
fun Message.attachmentText(): String? = when (val attachment = getAttachment()) {
    is CardAttachment -> {
        if (attachment.card != null) {
            // todo: translate
            "Sent a page"
        } else {
            null
        }
    }
    is PhotosAttachment -> {
        // todo: translate
       "Sent a photo"
    }
    is AudioAttachment -> {
        // todo: translate
        "Sent an audio message"
    }
    is VideosAttachment -> {
        // todo: translate
        "Sent a video"
    }
    is StoryAttachment -> {
        // todo: translate
        "Sent a story"
    }
    is GroupAttachment -> {
        // todo: translate
        "Sent a group"
    }
    is StickerAttachment -> {
        // todo: translate
        "Sent a sticker"
    }
    else -> null
}

@Composable
fun Message.preview(): String? {
    return text?.notBlank ?: attachmentText()
}
