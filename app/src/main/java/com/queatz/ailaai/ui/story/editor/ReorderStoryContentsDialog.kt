package com.queatz.ailaai.ui.story.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.card
import com.queatz.ailaai.ui.components.CardItem
import com.queatz.ailaai.ui.story.ReorderDialog
import com.queatz.ailaai.ui.story.StoryContent
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun ReorderStoryContentsDialog(
    navController: NavController,
    onDismissRequest: () -> Unit,
    storyContents: List<StoryContent>,
    onStoryContents: (List<StoryContent>) -> Unit
) {
    // todo, why can't just use storyContents?
    var currentStoryContents by remember { mutableStateOf(storyContents) }

    ReorderDialog(
        {
            onDismissRequest()
        },
        list = true,
        items = currentStoryContents,
        key = { it.key },
        onMove = { from, to ->
            onStoryContents(
                currentStoryContents.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }.also {
                    currentStoryContents = it
                }
            )
        },
        draggable = { it !is StoryContent.Title}
    ) { it, elevation ->
        when (it) {
            is StoryContent.Cards -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    items(it.cards, key = { it }) { cardId ->
                        var card by remember { mutableStateOf<Card?>(null) }

                        LaunchedEffect(cardId) {
                            api.card(cardId) { card = it }
                        }

                        CardItem(
                            onClick = null,
                            card = card,
                            isChoosing = true,
                            navController = navController,
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }
            }

            is StoryContent.Photos -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    items(it.photos, key = { it }) { photo ->
                        AsyncImage(
                            model = api.url(photo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.large)
                                .clip(MaterialTheme.shapes.large)
                                .height(106.dp)
                                .aspectRatio(it.aspect)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }
                }
            }

            is StoryContent.Audio -> {
                Card(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                        modifier = Modifier
                            .padding(PaddingDefault)
                    ) {
                        Icon(Icons.Outlined.PlayCircle, null)
                        Text(
                            stringResource(R.string.audio),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }

            is StoryContent.Title -> {
                Text(
                    it.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            is StoryContent.Section -> {
                Text(
                    it.section,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(PaddingDefault * 2)
                )
            }

            is StoryContent.Text -> {
                Text(
                    it.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(PaddingDefault * 2)
                )
            }

            else -> {
                // Unsupported
            }
        }
    }
}
