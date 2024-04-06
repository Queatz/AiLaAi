package com.queatz.ailaai.ui.story

import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.wordCount
import com.queatz.db.Story
import com.queatz.db.StoryContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class StoryPart(val type: String, val content: JsonObject)

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

inline fun <reified T : @Serializable StoryContent> T.toJsonStoryPart() = json.encodeToJsonElement(
    StoryPart(
        partType(),
        json.encodeToJsonElement(this).jsonObject
    )
)

fun JsonObject.toStoryContent(): StoryContent? = get("content")?.jsonObject?.let { content ->
    when (get("type")?.jsonPrimitive?.content) {
        "section" -> json.decodeFromJsonElement<StoryContent.Section>(content)
        "text" -> json.decodeFromJsonElement<StoryContent.Text>(content)
        "cards" -> json.decodeFromJsonElement<StoryContent.Cards>(content)
        "groups" -> json.decodeFromJsonElement<StoryContent.Groups>(content)
        "photos" -> json.decodeFromJsonElement<StoryContent.Photos>(content)
        "audio" -> json.decodeFromJsonElement<StoryContent.Audio>(content)
        "widget" -> try {
            json.decodeFromJsonElement<StoryContent.Widget>(content)
        } catch (e: SerializationException) {
            // Widget type unsupported
            // todo: show that app needs to be updated
            e.printStackTrace()
            null
        }
        "button" -> json.decodeFromJsonElement<StoryContent.Button>(content)

        else -> null
    }
}

fun Story.asContents() = listOf(
    StoryContent.Title(title ?: "", id!!),
    StoryContent.Authors(publishDate, authors ?: emptyList()),
) + contents()

fun Story.contents(): List<StoryContent> = (content ?: "[]").asStoryContents()

fun String.asStoryContents() = json
    .decodeFromString<List<JsonElement>>(this)
    .mapNotNull {
        it.jsonObject.toStoryContent()
    }

fun Story.asTextContent(): String = asContents().asText()

fun Story.textContent(): String = contents().asText()

fun List<StoryContent>.asText() = mapNotNull {
    when (it) {
        is StoryContent.Title -> it.title.notBlank
        is StoryContent.Section -> it.section.notBlank
        is StoryContent.Text -> it.text.notBlank
        else -> null
    }
}.joinToString("\n")

fun StoryContent.wordCount() = when (this) {
    is StoryContent.Title -> title.wordCount()
    is StoryContent.Section -> section.wordCount()
    is StoryContent.Text -> text.wordCount()
    else -> 0
}
