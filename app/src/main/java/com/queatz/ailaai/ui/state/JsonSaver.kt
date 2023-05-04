package com.queatz.ailaai.ui.state

import androidx.compose.runtime.saveable.Saver
import com.queatz.ailaai.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

inline fun <reified T> jsonSaver() = Saver<T, String>(
    { json.encodeToString(it).takeIf { it.length < 10_000 } }, // Cannot save large parcel
    { json.decodeFromString(it) }
)
