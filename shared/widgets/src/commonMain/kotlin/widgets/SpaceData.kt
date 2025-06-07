package com.queatz.widgets.widgets

import kotlinx.serialization.Serializable


@Serializable
data class SpaceData(
    var card: String? = null,
    var items: List<SpaceItem>? = null
)

@Serializable
data class SpaceItem(
    val content: SpaceContent,
    val position: Pair<Double, Double>
)

@Serializable
sealed class SpaceContent {
    @Serializable
    data class Page(
        val id: String
    ) : SpaceContent()

    @Serializable
    data class Text(
        val page: String? = null,
        val text: String? = null
    ) : SpaceContent()

    @Serializable
    data class Line(
        val page: String? = null,
        val to: Pair<Double, Double>
    ) : SpaceContent()

    @Serializable
    data class Box(
        val page: String? = null,
        val to: Pair<Double, Double>
    ) : SpaceContent()

    @Serializable
    data class Circle(
        val page: String? = null,
        val to: Pair<Double, Double>
    ) : SpaceContent()

    @Serializable
    data class Scribble(
        val page: String? = null,
        val points: List<Pair<Double, Double>>,
        val to: Pair<Double, Double>
    ) : SpaceContent()

    @Serializable
    data class Photo(
        val page: String? = null,
        val photo: String,
        val width: Int? = null,
        val height: Int? = null,
        val to: Pair<Double, Double>
    ) : SpaceContent()

    @Serializable
    data class Slide(
        val page: String? = null,
        val title: String? = null,
        val items: List<String> = emptyList()
    ) : SpaceContent()
}
