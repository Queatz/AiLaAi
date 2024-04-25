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
    object Divider : StoryContent()
    @Serializable
    class Reactions(var story: String, val reactions: ReactionSummary?) : StoryContent()
    @Serializable
    class Title(var title: String, val id: String) : StoryContent()
    @Serializable
    class Authors(var publishDate: Instant?, var authors: List<Person>) : StoryContent()
    @Serializable
    class Section(var section: String) : StoryContent()
    @Serializable
    class Text(var text: String) : StoryContent()
    @Serializable
    class Groups(var groups: List<String>) : StoryContent()
    @Serializable
    class Cards(var cards: List<String>) : StoryContent()
    @Serializable
    class Photos(var photos: List<String>, var aspect: Float = 0.75f) : StoryContent()
    @Serializable
    class Audio(var audio: String) : StoryContent()
    @Serializable
    class Widget(var widget: Widgets, var id: String) : StoryContent()
    @Serializable
    class Button(var text: String, var script: String, var data: String?) : StoryContent()
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
    else -> false
}
