package com.queatz.db

import com.queatz.widgets.Widgets
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
