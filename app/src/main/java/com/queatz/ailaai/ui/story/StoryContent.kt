package com.queatz.ailaai.ui.story

import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.Story
import com.queatz.ailaai.extensions.wordCount
import com.queatz.ailaai.data.json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.random.Random

@Serializable
sealed class StoryContent(val key: Long = Random.nextLong()) {
    object Divider : StoryContent()
    class Title(var title: String, val id: String) : StoryContent()
    class Authors(var publishDate: Instant?, var authors: List<Person>) : StoryContent()
    @Serializable class Section(var section: String) : StoryContent()
    @Serializable class Text(var text: String) : StoryContent()
    @Serializable class Cards(var cards: List<String>) : StoryContent()
    @Serializable class Photos(var photos: List<String>, var aspect: Float = 0.75f) : StoryContent()
    @Serializable class Audio(var audio: String) : StoryContent()
}

@Serializable
data class StoryPart(val type: String, val content: JsonObject)

fun StoryContent.partType() = when (this) {
    is StoryContent.Section -> "section"
    is StoryContent.Text -> "text"
    is StoryContent.Cards -> "cards"
    is StoryContent.Photos -> "photos"
    is StoryContent.Audio -> "audio"
    else -> throw NotImplementedError("$this is not a valid story part")
}

fun StoryContent.isPart() = when (this) {
    is StoryContent.Section -> true
    is StoryContent.Text -> true
    is StoryContent.Cards -> true
    is StoryContent.Photos -> true
    is StoryContent.Audio -> true
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
        "photos" -> json.decodeFromJsonElement<StoryContent.Photos>(content)
        "audio" -> json.decodeFromJsonElement<StoryContent.Audio>(content)
        else -> null
    }
}

fun Story.asContents() = listOf(
    StoryContent.Title(title ?: "", id!!),
    StoryContent.Authors(publishDate, authors ?: emptyList()),
) + contents()

fun Story.contents(): List<StoryContent> = json
    .decodeFromString<List<JsonElement>>(content ?: "[]")
    .mapNotNull {
        it.jsonObject.toStoryContent()
    }

fun Story.textContent(): String = contents().mapNotNull {
    when (it) {
        is StoryContent.Title -> it.title.takeIf { it.isNotBlank() }
        is StoryContent.Section -> it.section.takeIf { it.isNotBlank() }
        is StoryContent.Text -> it.text.takeIf { it.isNotBlank() }
        else -> null
    }
}.joinToString("\n")

fun StoryContent.wordCount() = when (this) {
    is StoryContent.Title -> title.wordCount()
    is StoryContent.Section -> section.wordCount()
    is StoryContent.Text -> text.wordCount()
    else -> 0
}
