package com.queatz.widgets.widgets

import kotlinx.serialization.Serializable

@Serializable
data class ScriptData(
    var script: String? = null,
    var data: String? = null
)
