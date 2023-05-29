package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.queatz.ailaai.Card
import com.queatz.ailaai.LinkifyText
import com.queatz.ailaai.api
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Audio
import com.queatz.ailaai.ui.components.CardItem
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun StoryContents(
    content: List<StoryContent>,
    state: LazyGridState,
    navController: NavController,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    onExpand: ((storyId: String) -> Unit)? = null,
) {
    var viewHeight by rememberStateOf(Float.MAX_VALUE)

    SelectionContainer {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                PaddingDefault * 2,
                0.dp,
                PaddingDefault * 2,
                PaddingDefault * 2 + bottomContentPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
            modifier = modifier
                .onPlaced {
                    viewHeight = it.boundsInParent().height
                }
        ) {
            content.forEach { content ->
                when (content) {
                    is StoryContent.Divider -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            DisableSelection {
                                Icon(
                                    Icons.Outlined.Flare,
                                    null,
                                    modifier = Modifier.padding(PaddingDefault * 2)
                                )
                            }
                        }
                    }

                    is StoryContent.Title -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                            ) {
                                Text(
                                    content.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .let {
                                            if (onExpand == null) {
                                                it
                                            } else {
                                                it.padding(top = PaddingDefault)
                                                    .clickable(
                                                        remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        onExpand(content.id)
                                                    }
                                            }
                                        }
                                )
                                if (onExpand != null) {
                                    IconButton(
                                        onClick = {
                                            onExpand(content.id)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.ArrowOutward,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is StoryContent.Section -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                content.section,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }

                    is StoryContent.Text -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LinkifyText(
                                content.text,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }

                    is StoryContent.Authors -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StoryAuthors(
                                navController,
                                content.publishDate,
                                content.authors
                            )
                        }
                    }

                    is StoryContent.Cards -> {
                        items(content.cards) {
                            DisableSelection {
                                var card by remember { mutableStateOf<Card?>(null) }
                                LaunchedEffect(Unit) {
                                    card = api.card(it)
                                }
                                CardItem(
                                    {
                                        navController.navigate("card/$it")
                                    },
                                    onCategoryClick = {},
                                    activity = navController.context as Activity,
                                    card = card,
                                    isChoosing = true,
                                    modifier = Modifier
                                        .fillMaxWidth(.75f)
                                        .heightIn(max = viewHeight.inDp())
                                )
                            }
                        }
                    }

                    is StoryContent.Photos -> {
                        itemsIndexed(
                            content.photos,
                            span = { index, item ->
                                GridItemSpan(if (index == 0) maxLineSpan else if (index % 3 == 1) 1 else maxCurrentLineSpan)
                            }
                        ) { index, it ->
                            DisableSelection {
                                AsyncImage(
                                    model = api.url(it),
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.large)
                                        .heightIn(min = 240.dp.coerceAtMost(viewHeight.inDp()), max = viewHeight.inDp())
                                        .fillMaxWidth()
                                        .aspectRatio(content.aspect)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                )
                            }
                        }
                    }

                    is StoryContent.Audio -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            DisableSelection {
                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.large)
                                ) {
                                    Audio(
                                        api.url(content.audio),
                                        modifier = Modifier
                                            .fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
