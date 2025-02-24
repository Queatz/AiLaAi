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
    class Page(
        val id: String
    ) : SpaceContent()

    @Serializable
    class Line(
        val page: String? = null,
        val to: Pair<Double, Double>
    ) : SpaceContent()
}
