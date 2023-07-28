package com.queatz.ailaai.ui.state

import androidx.compose.runtime.saveable.Saver
import com.queatz.ailaai.data.json
import kotlinx.serialization.encodeToString

inline fun <reified T> jsonSaver(default: T? = null) = Saver<T, String>(
    { json.encodeToString(it).takeIf { it.length < 10_000 } }, // Cannot save large parcel
    { json.decodeFromString(it) }
)
