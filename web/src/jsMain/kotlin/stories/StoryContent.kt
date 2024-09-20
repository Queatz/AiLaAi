package stories

import com.queatz.db.Story
import com.queatz.db.StoryContent
import json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import notBlank

// todo move these to ailaai-shared
fun JsonObject.toStoryContent(): StoryContent? = get("content")?.jsonObject?.let { content ->
    when (get("type")?.jsonPrimitive?.content) {
        "section" -> json.decodeFromJsonElement<StoryContent.Section>(content)
        "text" -> json.decodeFromJsonElement<StoryContent.Text>(content)
        "groups" -> json.decodeFromJsonElement<StoryContent.Groups>(content)
        "cards" -> json.decodeFromJsonElement<StoryContent.Cards>(content)
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


fun Story.full(): List<StoryContent> = contents().let { parts ->
    listOf(
        StoryContent.Title(title ?: "", id!!),
        StoryContent.Authors(publishDate, authors ?: emptyList()),
    ) + parts + (if (published == true) listOf(
        StoryContent.Reactions(id!!, reactions),
        StoryContent.Comments(id!!)
    ) else emptyList())
}
