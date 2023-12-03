package com.queatz.db

import kotlinx.serialization.Serializable

@Serializable
class CardAttachment(
    var card: String? = null
) : MessageAttachment() {
    override val type = "card"
}
@Serializable
class PhotosAttachment(
    var photos: List<String>? = null
) : MessageAttachment() {
    override val type = "photos"
}

@Serializable
class AudioAttachment(
    var audio: String? = null
) : MessageAttachment() {
    override val type = "audio"
}

@Serializable
class VideosAttachment(
    var videos: List<String>? = null
) : MessageAttachment() {
    override val type = "videos"
}

@Serializable
class ReplyAttachment(
    var message: String? = null
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
abstract class MessageAttachment  {
    abstract val type: String
}
