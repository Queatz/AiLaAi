package com.queatz.ailaai.ui.state

import androidx.compose.runtime.saveable.Saver
import com.google.gson.reflect.TypeToken
import com.queatz.ailaai.gson

inline fun <reified T> gsonSaver() = Saver<T, String>(
    { gson.toJson(it) },
    { gson.fromJson(it, object : TypeToken<T>() {}.type) }
)
