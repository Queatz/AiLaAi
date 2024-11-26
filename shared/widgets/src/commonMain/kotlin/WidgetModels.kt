package com.queatz.widgets

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FormValue(
    val key: String,
    val title: String,
    val value: JsonElement
)
