package com.queatz.ailaai.ui.story

import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.wordCount
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.toJsonStoryPart
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

inline fun <reified T : @Serializable StoryContent> T.toJsonStoryPart() = toJsonStoryPart(json)

fun JsonObject.toStoryContent(): StoryContent? = get("content")?.jsonObject?.let { content ->
    when (get("type")?.jsonPrimitive?.content) {
        "section" -> json.decodeFromJsonElement<StoryContent.Section>(content)
        "text" -> json.decodeFromJsonElement<StoryContent.Text>(content)
        "cards" -> json.decodeFromJsonElement<StoryContent.Cards>(content)
        "groups" -> json.decodeFromJsonElement<StoryContent.Groups>(content)
        "photos" -> json.decodeFromJsonElement<StoryContent.Photos>(content)
        "video" -> json.decodeFromJsonElement<StoryContent.Video>(content)
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
        "profile" -> json.decodeFromJsonElement<StoryContent.Profiles>(content)

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

fun StoryContent.isNotBlank() = when (this) {
    is StoryContent.Title -> title.isNotBlank()
    is StoryContent.Section -> section.isNotBlank()
    is StoryContent.Text -> text.isNotBlank()
    is StoryContent.Photos -> photos.isNotEmpty()
    is StoryContent.Video -> video.isNotEmpty()
    is StoryContent.Groups -> groups.isNotEmpty()
    is StoryContent.Cards -> cards.isNotEmpty()
    else -> true
}
