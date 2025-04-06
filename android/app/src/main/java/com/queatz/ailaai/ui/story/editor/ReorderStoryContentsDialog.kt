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
import app.ailaai.api.card
import app.ailaai.api.group
import app.ailaai.api.profile
import coil3.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.story.ReorderDialog
import com.queatz.ailaai.ui.story.Stub
import com.queatz.ailaai.ui.story.WidgetStub
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import com.queatz.db.StoryContent

@Composable
fun ReorderStoryContentsDialog(
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
        key = { it.hashCode().toString() },
        onMove = { from, to ->
            onStoryContents(
                currentStoryContents.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }.also {
                    currentStoryContents = it
                }
            )
        },
        draggable = { it !is StoryContent.Title }
    ) { it, elevation ->
        when (it) {
            is StoryContent.Cards -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
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
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }
            }

            is StoryContent.Groups -> {
                Card(
                    shape = MaterialTheme.shapes.large
                ) {
                    it.groups.firstOrNull()?.let { groupId ->
                        var group by remember { mutableStateOf<GroupExtended?>(null) }

                        LaunchedEffect(groupId) {
                            api.group(groupId) { group = it }
                        }

                        LoadingText(group != null, stringResource(R.string.loading_group)) {
                            ContactItem(
                                onClick = null,
                                onLongClick = null,
                                item = SearchResult.Group(group!!),
                                info = GroupInfo.LatestMessage
                            )
                        }
                    }
                }
            }

            is StoryContent.Photos -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
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
                                .then(
                                    if (it.aspect != null) {
                                        Modifier
                                            .aspectRatio(it.aspect!!)
                                    } else {
                                        Modifier
                                    }
                                )
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
                        horizontalArrangement = Arrangement.spacedBy(1.pad),
                        modifier = Modifier
                            .padding(1.pad)
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
                        .padding(2.pad)
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
                        .padding(2.pad)
                )
            }

            is StoryContent.Widget -> {
                WidgetStub(it)
            }

            is StoryContent.Button -> {
                Stub(it.text)
            }

            is StoryContent.Input -> {
                Stub(it.key)
            }

            is StoryContent.Profiles -> {
                LazyRow {
                    items(it.profiles, key = { it }) { profileId ->
                        var person by remember { mutableStateOf<Person?>(null) }

                        LaunchedEffect(profileId) {
                            api.profile(profileId) { person = it.person }
                        }

                        GroupPhoto(
                            person?.let { person ->
                                listOf(
                                    ContactPhoto(
                                        person.name.orEmpty(),
                                        person.photo,
                                        person.seen
                                    )
                                )
                            } ?: emptyList()
                        )
                    }
                }
            }

            else -> {
                // Unsupported
            }
        }
    }
}
