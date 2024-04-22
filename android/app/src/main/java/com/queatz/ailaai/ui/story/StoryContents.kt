package com.queatz.ailaai.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ailaai.api.card
import app.ailaai.api.group
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.reactToStory
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.AddReactionDialog
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.script.ScriptContent
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.GroupExtended
import com.queatz.db.ReactBody
import com.queatz.db.Reaction
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryContents(
    source: StorySource?,
    content: List<StoryContent>,
    state: LazyGridState,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.pad,
    horizontalPadding: Dp = 2.pad,
    fade: Boolean = false,
    onButtonClick: ((script: String, data: String?) -> Unit)? = null,
    actions: (@Composable (storyId: String) -> Unit)? = null
) {
    var viewHeight by rememberStateOf(Float.MAX_VALUE)
    var showOpenWidgetDialog by rememberStateOf(false)
    var size by rememberStateOf(Size.Zero)
    val nav = nav
    val scope = rememberCoroutineScope()

    if (showOpenWidgetDialog) {
        val context = LocalContext.current
        AlertDialog(
            {
                showOpenWidgetDialog = false
            },
            text = {
                // todo translate
               Text("This widget is currently only interactable on web.")
            },
            dismissButton = {
                TextButton(
                    {
                        showOpenWidgetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            },
            confirmButton = {
                TextButton(
                    {
                        showOpenWidgetDialog = false
                        Card().apply { id = (source as StorySource.Card).id }.url.launchUrl(context)
                    }
                ) {
                    Text(stringResource(R.string.open_card))
                }
            }
        )
    }

    SelectionContainer(modifier = modifier) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                horizontalPadding,
                0.dp,
                horizontalPadding,
                2.pad + bottomContentPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
            modifier = Modifier
                .onPlaced {
                    size = it.boundsInParent().size
                    viewHeight = it.boundsInParent().height
                }
                .let {
                    if (fade) {
                        it.fadingEdge(size, state, 12f)
                    } else {
                        it
                    }
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
                                    modifier = Modifier.padding(2.pad)
                                )
                            }
                        }
                    }

                    is StoryContent.Reactions -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            DisableSelection {
                                var showAddReactionDialog by rememberStateOf(false)

                                if (showAddReactionDialog) {
                                    AddReactionDialog(
                                        {
                                            showAddReactionDialog = false
                                        }
                                    ) { reaction ->
                                        scope.launch {
                                            api.reactToStory(
                                                content.story,
                                                ReactBody(Reaction(reaction = reaction, comment = null))
                                            ) {

                                            }
                                        }
                                        showAddReactionDialog = false
                                    }
                                }

                                FlowRow(
                                    verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
                                    horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    if (content.reactions?.all.isNullOrEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    api.reactToStory(
                                                        content.story,
                                                        ReactBody(Reaction(reaction = "❤", comment = null))
                                                    ) {

                                                    }
                                                }
                                            }
                                        ) {
                                            Text("❤", style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                "0",
                                                modifier = Modifier
                                                    .padding(start = .5f.pad)
                                            )
                                        }
                                    } else {
                                        content.reactions!!.all.forEach { reaction ->
                                            val mine = content.reactions!!.mine?.any { it.reaction == reaction.reaction} == true

                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        api.reactToStory(
                                                            content.story,
                                                            ReactBody(
                                                                Reaction(
                                                                    reaction = reaction.reaction
                                                                ),
                                                                remove = true
                                                            )
                                                        ) {

                                                        }
                                                    }
                                                },
                                                colors = if (mine) ButtonDefaults.outlinedButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                ) else ButtonDefaults.outlinedButtonColors()
                                            ) {
                                                Text(reaction.reaction, style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    reaction.count.formatMini(),
                                                    style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier
                                                        .padding(start = .5f.pad)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            showAddReactionDialog = true
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            Icons.Outlined.AddReaction,
                                            null,
                                            modifier = Modifier
                                                .fillMaxHeight()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is StoryContent.Title -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.pad),
                            ) {
                                Text(
                                    content.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .let {
                                            if (actions == null) {
                                                it
                                            } else {
                                                it.padding(top = 1.pad)
                                                    .clickable(
                                                        remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        nav.navigate(AppNav.Story(content.id))
                                                    }
                                            }
                                        }
                                )
                                // https://issuetracker.google.com/issues/300781578
                                DisableSelection {
                                    actions?.invoke(content.id)
                                }
                            }
                        }
                    }

                    is StoryContent.Section -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                content.section,
                                style = MaterialTheme.typography.titleLarge,
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
                                nav,
                                content.publishDate,
                                content.authors
                            )
                        }
                    }

                    is StoryContent.Groups -> {
                        items(content.groups, span = { GridItemSpan(maxLineSpan) }) { groupId ->
                            DisableSelection {
                                var group by remember { mutableStateOf<GroupExtended?>(null) }

                                LaunchedEffect(groupId) {
                                    api.group(groupId) { group = it }
                                }

                                LoadingText(group != null, stringResource(R.string.loading_group)) {
                                    ContactItem(
                                        SearchResult.Group(group!!),
                                        onChange = {},
                                        info = GroupInfo.LatestMessage
                                    )
                                }
                            }
                        }
                    }

                    is StoryContent.Cards -> {
                        items(content.cards) {
                            DisableSelection {
                                var card by remember { mutableStateOf<Card?>(null) }
                                LaunchedEffect(Unit) {
                                    api.card(it) { card = it }
                                }
                                CardItem(
                                    {
                                        nav.navigate(AppNav.Page(it))
                                    },
                                    onCategoryClick = {
                                        exploreInitialCategory = it
                                        nav.navigate(AppNav.Explore)
                                    },
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
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(api.url(it))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.large)
                                        .fillMaxWidth()
                                        .aspectRatio(content.aspect)
                                        .heightIn(min = 240.dp.coerceAtMost(viewHeight.inDp()), max = viewHeight.inDp())
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

                    is StoryContent.Widget -> {
                        when (content.widget) {
                            Widgets.Script -> {
                                ScriptContent(content.id)
                            }
                            else -> {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    DisableSelection {
                                        Stub(content.widget.stringResource) {
                                            if (source is StorySource.Card) {
                                                showOpenWidgetDialog = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is StoryContent.Button -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Button(
                                onClick = {
                                    onButtonClick?.invoke(content.script, content.data)
                                }
                            ) {
                                Text(content.text)
                            }
                        }
                    }
                }
            }
        }
    }
}
