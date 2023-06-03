package com.queatz.ailaai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.queatz.ailaai.api.presence
import com.queatz.ailaai.api.readStoriesUntilNow
import kotlinx.coroutines.flow.MutableStateFlow

val mePresence = MePresence()

class MePresence {

    private val value = MutableStateFlow<Presence?>(null)

    @Composable
    fun rememberPresence(): State<Presence?> = value.collectAsState()

    suspend fun reload() {
        // Todo retry on error
        api.presence { value.value = it }
    }

    suspend fun readStoriesUntilNow() {
        api.readStoriesUntilNow()
        reload()
    }
}
