package stories

import com.queatz.db.Person
import com.queatz.db.Story
import com.queatz.widgets.Widgets
import json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import notBlank

// todo move these to ailaai-shared
@Serializable
sealed class StoryContent {
    object Divider : StoryContent()
    class Title(var title: String, val id: String) : StoryContent()
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
}

// todo move these to ailaai-shared
fun JsonObject.toStoryContent(): StoryContent? = get("content")?.jsonObject?.let { content ->
    when (get("type")?.jsonPrimitive?.content) {
        "section" -> json.decodeFromJsonElement<StoryContent.Section>(content)
        "text" -> json.decodeFromJsonElement<StoryContent.Text>(content)
        "groups" -> json.decodeFromJsonElement<StoryContent.Groups>(content)
        "cards" -> json.decodeFromJsonElement<StoryContent.Cards>(content)
        "photos" -> json.decodeFromJsonElement<StoryContent.Photos>(content)
        "audio" -> json.decodeFromJsonElement<StoryContent.Audio>(content)
        "widget" -> json.decodeFromJsonElement<StoryContent.Widget>(content)
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
    ) + parts
}
