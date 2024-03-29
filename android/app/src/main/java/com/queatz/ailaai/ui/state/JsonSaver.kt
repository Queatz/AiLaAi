package com.queatz.ailaai.ui.state

import androidx.compose.runtime.saveable.Saver
import com.queatz.ailaai.data.json
import kotlinx.serialization.encodeToString

inline fun <reified T : Any?> jsonSaver(default: T? = null) = Saver<T, String>(
    { json.encodeToString(it).takeIf { it.length < 10_000 } ?: "" }, // Cannot save large parcel // todo store in-memory i.e. ::123456 (but then when to discard)
    { it.takeIf { it.isNotEmpty() }?.let { json.decodeFromString(it) } ?: default }
)
