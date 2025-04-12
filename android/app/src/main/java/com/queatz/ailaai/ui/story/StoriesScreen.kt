package com.queatz.ailaai.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.helpers.LocationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.CardList
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.SearchFilter
import com.queatz.ailaai.ui.control.MapCardsControl
import com.queatz.ailaai.ui.screens.GroupsScreen
import com.queatz.ailaai.ui.screens.SearchContent
import com.queatz.ailaai.ui.sheet.SheetHeader
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Story
import com.queatz.db.StoryContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var cache = emptyList<Story>()

class StoriesScreenState() {

    internal var state: LazyGridState? = null

    suspend fun scrollToTop() {
        state?.scrollToTop()
    }
}

enum class SheetContent {
    Posts,
    Groups,
    Events,
    Pages
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoriesScreen(
    mapCardsControl: MapCardsControl,
    geo: LatLng?,
    title: String? = null,
    onTitleClick: (() -> Unit)? = null,
    onExpandRequest: () -> Unit = {},
    storiesState: StoriesScreenState = remember { StoriesScreenState() },
    value: String,
    locationSelector: LocationSelector,
    filters: List<SearchFilter>,
    valueChange: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val context = LocalContext.current
    var stories by remember { mutableStateOf(cache) }
    val storyContents = remember(stories) {
        stories.flatMapIndexed { index, story ->
            (if (index > 0) listOf(StoryContent.Divider) else emptyList()) +
                    story.asContents() +
                    listOf(
                        StoryContent.Reactions(story.id!!, story.reactions),
                        StoryContent.Comments(story.id!!)
                    )
        }
    }
    var isLoading by rememberStateOf(stories.isEmpty())
    val nav = nav
    var showShareAThought by rememberStateOf(true)
    var sheetContent by rememberSavableStateOf(SheetContent.Posts)

    LaunchedEffect(state) {
        storiesState.state = state
    }

    LaunchedEffect(stories) {
        cache = stories
    }

    LaunchedEffect(Unit) {
        delay(7_000)
        mePresence.readStoriesUntilNow()
    }

    suspend fun reload() {
        if (geo != null) {
            api.stories(
                geo = geo.toGeo(),
                public = true,
                onError = {
                    if (it is CancellationException) {
                        // Ignored, geo probably changes
                    } else {
                        isLoading = false
                        context.showDidntWork()
                    }
                }
            ) {
                stories = it
                isLoading = false
            }
        }
    }

    LaunchedEffect(geo) {
        reload()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (sheetContent) {
                SheetContent.Posts -> {
                    StoryContents(
                        source = null,
                        content = storyContents,
                        horizontalPadding = 1.pad,
                        state = state,
                        onReloadRequest = {
                            scope.launch {
                                reload()
                            }
                        },
                        onCommentFocused = {
                            showShareAThought = !it
                        },
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .weight(1f),
                        bottomContentPadding = 80.dp,
                        header = {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SheetHeader(
                                    title = title,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest,
                                    isLoading = isLoading,
                                    isEmpty = storyContents.isEmpty()
                                )
                            }
                        }
                    ) { storyId ->
                        Row {
                            StoryActions(
                                storyId,
                                stories.find { it.id == storyId },
                                showOpen = true
                            )
                        }
                    }
                }

                SheetContent.Pages -> {
                    CardList(
                        state = state,
                        cards = mapCardsControl.mapCategoriesControl.cardsOfCategory,
                        geo = geo,
                        isLoading = isLoading,
                        isError = mapCardsControl.isError,
                        value = value,
                        valueChange = valueChange,
                        placeholder = stringResource(R.string.search),
                        hasMore = mapCardsControl.hasMore,
                        onLoadMore = {
                            mapCardsControl.loadMore(false)
                        },
                        action = {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.your_cards))
                        },
                        onAction = {
                            nav.appNavigate(AppNav.Me)
                        },
                        header = {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SheetHeader(
                                    title = title,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest,
                                    isLoading = isLoading,
                                    isEmpty = storyContents.isEmpty()
                                )
                            }
                        },
                        aboveSearchFieldContent = {
                            SearchContent(
                                locationSelector = locationSelector,
                                isLoading = isLoading,
                                filters = filters,
                                categories = mapCardsControl.mapCategoriesControl.categories,
                                category = mapCardsControl.mapCategoriesControl.selectedCategory,
                                onCategory = {
                                    mapCardsControl.mapCategoriesControl.selectCategory(it)
                                }
                            )
                        }
                    )
                }

                SheetContent.Groups -> {
                    GroupsScreen(
                        geo = geo?.toGeo(),
                        locationSelector = locationSelector,
                        header = {
                            item {
                                SheetHeader(
                                    title = title,
                                    onTitleClick = onTitleClick,
                                    selected = sheetContent,
                                    onSelected = { sheetContent = it },
                                    onExpandRequest = onExpandRequest,
                                    isLoading = isLoading,
                                    isEmpty = storyContents.isEmpty()
                                )
                            }
                        }
                    )
                }
                else -> Unit
            }
        }

        var thought by rememberStateOf("")

        AnimatedVisibility(
            visible = showShareAThought && sheetContent == SheetContent.Posts,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            PageInput {
                SearchFieldAndAction(
                    value = thought,
                    valueChange = { thought = it },
                    placeholder = stringResource(R.string.share_a_thought),
                    showClear = false,
                    singleLine = false,
                    action = {
                        if (thought.isBlank()) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.your_stories))
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(R.string.write_a_story))
                        }
                    },
                    onAction = {
                        if (thought.isBlank()) {
                            nav.appNavigate(AppNav.Write)
                        } else {
                            scope.launch {
                                api.createStory(Story(title = thought)) {
                                    nav.appNavigate(AppNav.WriteStory(it.id!!))
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
