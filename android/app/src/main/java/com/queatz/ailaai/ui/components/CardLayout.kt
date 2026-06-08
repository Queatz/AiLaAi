package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.LatLng
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.reply
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.card.CardContent
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
    largeTitle: Boolean = false,
    showAuthors: Boolean = true,
    aspect: Float = 1.5f,
    onClick: () -> Unit = {},
    onReply: ((List<String>) -> Unit)? = null,
    scope: CoroutineScope,
    elevation: Int = 1,
    showDistance: LatLng? = null,
    playVideo: Boolean = false,
    hideCreators: List<String>? = null,
) {
    val nav = nav
    val allPhotos = remember(card?.photo, card?.photos) {
        listOfNotNull(card?.photo) + (card?.photos ?: emptyList())
    }

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
        } else if (allPhotos.size > 1) {
            val pagerState = rememberPagerState { allPhotos.size }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(api.url(allPhotos[index]))
                            .crossfade(true)
                            .build(),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(elevation.elevation))
                            .clickable {
                                onClick()
                            }
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(allPhotos.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
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
        card?.let { card ->
            CardConversation(
                card = card,
                interactable = true,
                showTitle = showTitle,
                largeTitle = largeTitle,
                showAuthors = showAuthors,
                hideCreators = hideCreators,
                showDistance = showDistance,
                onReply = { conversation ->
                    if (onReply == null) {
                        scope.launch {
                            card.reply(conversation) { groupId ->
                                nav.appNavigate(AppNav.Group(groupId))
                            }
                        }
                    } else {
                        onReply(conversation)
                    }
                },
                onTitleClick = {
                    onClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(1.5f.pad)
            )

            card.content?.notBlank?.let { content ->
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
