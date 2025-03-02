package com.queatz.ailaai.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import app.ailaai.api.myGeo
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.SwipeResult
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.swipe
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.swipeMainTabs
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

@Composable
fun StoriesScreen(
    geo: LatLng?,
    storiesState: StoriesScreenState = remember { StoriesScreenState() }
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

    LaunchedEffect(state) {
        storiesState.state = state
    }

    LaunchedEffect(stories) {
        cache = stories
    }

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it.toGeo())
        }
    }

    LaunchedEffect(Unit) {
        delay(7_000)
        mePresence.readStoriesUntilNow()
    }

    suspend fun reload() {
        if (geo != null) {
            api.stories(
                geo.toGeo(),
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
            .swipeMainTabs {
                when (emptyList<Unit>().swipe(Unit, it)) {
                    is SwipeResult.Previous -> {
                        nav.appNavigate(AppNav.Messages)
                    }

                    is SwipeResult.Next -> {
                        nav.appNavigate(AppNav.Explore)
                    }

                    is SwipeResult.Select<*> -> Unit
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Loading(
                    modifier = Modifier
                        .padding(1.pad)
                )
            } else if (storyContents.isEmpty()) {
                EmptyText(stringResource(R.string.no_stories_to_read))
            } else {
                StoryContents(
                    source = null,
                    content = storyContents,
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
                    bottomContentPadding = 80.dp
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
        }

        var thought by rememberStateOf("")

        AnimatedVisibility(
            visible = showShareAThought,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            PageInput {
                SearchFieldAndAction(
                    thought,
                    { thought = it },
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
