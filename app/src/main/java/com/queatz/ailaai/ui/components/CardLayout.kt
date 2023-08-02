package com.queatz.ailaai.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.LatLng
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.reply
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CardLayout(
    card: Card?,
    isMine: Boolean,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    aspect: Float = 1.5f,
    onClick: () -> Unit,
    onChange: () -> Unit,
    scope: CoroutineScope,
    navController: NavController,
    elevation: Int = 1,
    showDistance: LatLng? = null,
    playVideo: Boolean = false,
    showToolbar: Boolean = false
) {
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
//                    .clip(MaterialTheme.shapes.large)
                    .aspectRatio(aspect)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationDefault * elevation))
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
//                        .clip(MaterialTheme.shapes.large)
                        .aspectRatio(aspect)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationDefault * elevation))
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
                navController = navController,
                showDistance = showDistance,
                onCategoryClick = {
                    exploreInitialCategory = it
                    navController.navigate("explore")
                },
                onReply = { conversation ->
                    scope.launch {
                        it.reply(conversation) { groupId ->
                            navController.navigate("group/${groupId}")
                        }
                    }
                },
                onTitleClick = {
                    onClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingDefault * 1.5f)
            )
            if (isMine && showToolbar) {
                CardToolbar(
                    navController = navController,
                    navController.context as Activity,
                    onChange,
                    it,
                    modifier = Modifier.padding(horizontal = PaddingDefault * 1.5f)
                )
            }
        }
    }
}
