package com.queatz.widgets.widgets

import kotlinx.serialization.Serializable

@Serializable
data class PageTreeData(
    var card: String? = null,
    var votes: Map<String, Int> = emptyMap(),
    var categories: Map<String, List<String>> = emptyMap(),
    var tags: Map<String, List<String>> = emptyMap(),
    var stages: Map<String, String> = emptyMap(),
)
