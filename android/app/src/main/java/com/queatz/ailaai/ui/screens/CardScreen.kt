package com.queatz.ailaai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.ailaai.api.card
import com.queatz.ailaai.background
import com.queatz.ailaai.data.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.status
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.slideshow.slideshow
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

@Composable
fun CardScreen(
    cardId: String,
    startInFullscreen: Boolean = false
) {
    val state = rememberCardScreenState(cardId)
    val slideshow = slideshow
    val slideshowActive by slideshow.active.collectAsState()
    val fullscreen by slideshow.fullscreen.collectAsState()
    val showInFullscreen = fullscreen || slideshowActive
    val context = LocalContext.current

    LaunchedEffect(startInFullscreen) {
        if (startInFullscreen) {
            slideshow.setFullscreen(true)
        }
    }

    BackHandler(fullscreen) {
        slideshow.setFullscreen(false)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (fullscreen) {
                slideshow.setFullscreen(false)
            }
        }
    }

    ResumeEffect(skipFirst = true) {
        state.reload()
        state.reloadCards()
    }

    LaunchedEffect(cardId) {
        if (state.card != null) return@LaunchedEffect
        state.isLoading = true
        state.notFound = false

        slideshow.card.value?.takeIf { it.id!! == cardId }.let {
            if (it != null) {
                state.card = it
            } else {
                api.card(cardId, onError = {
                    if (it.status == HttpStatusCode.NotFound) {
                        state.notFound = true
                    }
                }) {
                    state.card = it
                }
            }
        }

        state.reloadCards()
        state.isLoading = false
    }

    LaunchedEffect(Unit) {
        state.showTools = context.dataStore.data.first().let {
            it[booleanPreferencesKey("ui.showMyCardTools")] ?: true
        }
    }

    LaunchedEffect(state.showScanMe) {
        if (state.showScanMe) {
            slideshow.cancelUserInteraction()
            delay(2.seconds)
            state.showScanMe = false
            slideshow.cancelUserInteraction()
        }
    }

    LaunchedEffect(state.oldPhoto) {
        if (state.oldPhoto == null) return@LaunchedEffect

        var tries = 0
        while (tries++ < 5 && state.oldPhoto != null) {
            delay(3.seconds)
            api.card(cardId) {
                if (if (state.oldPhoto.isNullOrBlank()) !it.photo.isNullOrBlank() else it.photo != state.oldPhoto) {
                    state.reload()
                    state.oldPhoto = null
                }
            }
        }
    }

    background(state.card?.background?.takeIf { showInFullscreen }?.let(api::url))

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        CardAppBar(state, showInFullscreen)
        CardGridContent(state, showInFullscreen)
    }

    CardDialogs(state)
}
