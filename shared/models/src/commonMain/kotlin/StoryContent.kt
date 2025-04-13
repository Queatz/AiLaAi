package com.queatz.db

import com.queatz.widgets.Widgets
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
sealed class StoryContent {
    @Serializable
    data object Divider : StoryContent()
    @Serializable
    data class Reactions(val story: String, val reactions: ReactionSummary?) : StoryContent()
    @Serializable
    data class Comments(val story: String) : StoryContent()
    @Serializable
    data class Title(val title: String, val id: String) : StoryContent()
    @Serializable
    data class Authors(val publishDate: Instant?, val authors: List<Person>) : StoryContent()
    @Serializable
    data class Section(val section: String) : StoryContent()
    @Serializable
    data class Text(val text: String) : StoryContent()
    @Serializable
    data class Groups(val groups: List<String>, val coverPhotos: Boolean = false) : StoryContent()
    @Serializable
    data class Cards(val cards: List<String>) : StoryContent()
    @Serializable
    data class Photos(val photos: List<String>, val aspect: Float? = null) : StoryContent()
    @Serializable
    data class Audio(val audio: String) : StoryContent()
    @Serializable
    data class Widget(val widget: Widgets, val id: String) : StoryContent()
    @Serializable
    data class Button(val text: String, val script: String, val data: String?, val style: ButtonStyle? = null) : StoryContent()
    @Serializable
    data class Input(val key: String, val value: String?) : StoryContent()
    @Serializable
    data class Profiles(val profiles: List<String>) : StoryContent()
}

@Serializable
data class StoryPart(val type: String, val content: JsonObject)

inline fun <reified T : @Serializable StoryContent> T.toJsonStoryPart(json: Json) = json.encodeToJsonElement(
    StoryPart(
        partType(),
        json.encodeToJsonElement(this).jsonObject
    )
)

inline fun <reified T : @Serializable StoryContent> List<T>.toJsonStoryContent(json: Json) = json.encodeToString(
    buildJsonArray {
        filter { it.isPart() }.forEach { part ->
            add(part.toJsonStoryPart(json))
        }
    }
)

fun StoryContent.partType() = when (this) {
    is StoryContent.Section -> "section"
    is StoryContent.Text -> "text"
    is StoryContent.Cards -> "cards"
    is StoryContent.Groups -> "groups"
    is StoryContent.Photos -> "photos"
    is StoryContent.Audio -> "audio"
    is StoryContent.Widget -> "widget"
    is StoryContent.Button -> "button"
    is StoryContent.Input -> "input"
    is StoryContent.Profiles -> "profile"
    else -> throw NotImplementedError("$this is not a valid story part")
}

fun StoryContent.isPart() = when (this) {
    is StoryContent.Section -> true
    is StoryContent.Text -> true
    is StoryContent.Cards -> true
    is StoryContent.Groups -> true
    is StoryContent.Photos -> true
    is StoryContent.Audio -> true
    is StoryContent.Widget -> true
    is StoryContent.Button -> true
    is StoryContent.Input -> true
    is StoryContent.Profiles -> true
    else -> false
}
