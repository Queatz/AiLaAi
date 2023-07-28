package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.data.api
import com.queatz.ailaai.ui.components.Video
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.serialization.Serializable

@Serializable
sealed class Media {
    @Serializable
    data class Video(val url: String) : Media()
    @Serializable
    data class Photo(val url: String) : Media()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoDialog(onDismissRequest: () -> Unit, initialMedia: Media, medias: List<Media>) {
    Dialog(
        {
            onDismissRequest()
        }, properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ) {
                    onDismissRequest()
                }
        ) {
            val state = rememberLazyListState(medias.indexOf(initialMedia).coerceAtLeast(0))
            val current by remember {
                derivedStateOf {
                    state.firstVisibleItemIndex
                }
            }
            LazyRow(
                state = state,
                flingBehavior = rememberSnapFlingBehavior(state),
                reverseLayout = true,
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault * 2),
                modifier = Modifier.fillMaxSize()
            ) {
                items(medias) { media ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    ) {
                        when (media) {
                            is Media.Photo -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(api.url(media.url))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                )
                            }
                            is Media.Video -> {
                                Video(
                                    api.url(media.url),
                                    modifier = Modifier
                                        .fillParentMaxSize(),
                                    isPlaying = current == medias.indexOf(initialMedia)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
