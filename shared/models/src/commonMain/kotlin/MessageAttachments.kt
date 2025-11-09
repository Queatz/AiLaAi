package com.queatz.db

import kotlinx.serialization.Serializable

@Serializable
class CardAttachment(
    var card: String? = null
) : MessageAttachment() {
    override val type = "card"
}

@Serializable
class ProfilesAttachment(
    var profiles: List<String>? = null
) : MessageAttachment() {
    override val type = "profiles"
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
class GroupAttachment(
    var group: String? = null,
) : MessageAttachment() {
    override val type = "group"
}

@Serializable
class TradeAttachment(
    var trade: String? = null,
) : MessageAttachment() {
    override val type = "trade"
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
class UrlAttachment(
    var url: String? = null,
    var image: String? = null,
    var title: String? = null,
    var description: String? = null,
) : MessageAttachment() {
    override val type = "url"
}

@Serializable
class CallAttachment(
    var call: String? = null
) : MessageAttachment() {
    override val type = "call"
}

@Serializable
abstract class MessageAttachment  {
    abstract val type: String
}
