package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.LatLng
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.reply
import com.queatz.ailaai.nav
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.ailaai.ui.card.CardContent
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.theme.elevation
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CardLayout(
    card: Card?,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    aspect: Float = 1.5f,
    onClick: () -> Unit = {},
    scope: CoroutineScope,
    elevation: Int = 1,
    showDistance: LatLng? = null,
    playVideo: Boolean = false,
    hideCreators: List<String>? = null,
) {
    val nav = nav

    Column(
        modifier = modifier
            .shadow(1.dp, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.background)
            .clickable {
                onClick()
            }
    ) {
        val video = card?.video
        if (video != null) {
            Video(
                video.let(api::url),
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .aspectRatio(aspect)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(elevation.elevation))
                    .clickable {
                        onClick()
                    },
                isPlaying = playVideo
            )
        } else {
            card?.photo?.also {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(api.url(it))
                        .crossfade(true)
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .aspectRatio(aspect)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(elevation.elevation))
                        .clickable {
                            onClick()
                        }
                )
            }
        }
        card?.let {
            CardConversation(
                card,
                interactable = true,
                showTitle = showTitle,
                hideCreators = hideCreators,
                showDistance = showDistance,
                onCategoryClick = {
                    exploreInitialCategory = it
                    nav.navigate(AppNav.Explore)
                },
                onReply = { conversation ->
                    scope.launch {
                        it.reply(conversation) { groupId ->
                            nav.navigate(AppNav.Group(groupId))
                        }
                    }
                },
                onTitleClick = {
                    onClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.5f.pad)
            )

            card.content?.let { content ->
                Box(
                    modifier = Modifier
                        .heightIn(max = 2096.dp)
                ) {
                    CardContent(
                        StorySource.Card(card.id!!),
                        content
                    )
                }
            }
        }
    }
}
