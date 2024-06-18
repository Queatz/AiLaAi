package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import app.ailaai.api.myScripts
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.SwipeResult
import com.queatz.ailaai.extensions.isAtTop
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.swipe
import com.queatz.ailaai.extensions.toGeo
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.ButtonBar
import com.queatz.ailaai.ui.components.DisplayText
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.LocationScaffold
import com.queatz.ailaai.ui.components.PageInput
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.swipeMainTabs
import com.queatz.ailaai.ui.scripts.PreviewScriptAction
import com.queatz.ailaai.ui.scripts.ScriptsDialog
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Script
import com.queatz.db.Story
import com.queatz.db.StoryContent
import com.queatz.db.toJsonStoryContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var cache = emptyList<Story>()

@Composable
fun StoriesScreen() {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val context = LocalContext.current
    var geo by remember { mutableStateOf<LatLng?>(null) }
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        nav.context as Activity
    )
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
    var myScripts by rememberStateOf(emptyList<Script>())
    var showScriptsDialog by rememberStateOf(false)
    var showShareAThought by rememberStateOf(true)

    LaunchedEffect(Unit) {
        api.myScripts {
            myScripts = it
        }
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
                geo!!.toGeo(),
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

    LocationScaffold(
        geo,
        locationSelector,
        appHeader = {
            AppHeader(
                stringResource(R.string.explore),
                {}
            ) {
                ScanQrCodeButton()
            }
        },
        rationale = {
            // todo: translate
            DisplayText("Share and discover what's new in town.")
        }
    ) {
        val isAtTop by state.isAtTop()

        if (showScriptsDialog) {
            ScriptsDialog(
                {
                    showScriptsDialog = false
                },
                previewScriptAction = PreviewScriptAction.Edit
            )
        }

        Column {
            AppHeader(
                stringResource(R.string.explore),
                {
                    scope.launch {
                        state.scrollToTop()
                    }
                }
            ) {
                ScanQrCodeButton()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .swipeMainTabs {
                        when (emptyList<Unit>().swipe(Unit, it)) {
                            is SwipeResult.Previous -> {
                                nav.navigate(AppNav.Inventory)
                            }
                            is SwipeResult.Next -> {
                                nav.navigate(AppNav.Messages)
                            }
                            is SwipeResult.Select<*> -> {
                                // Impossible
                            }
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    AnimatedVisibility(myScripts.isNotEmpty() && isAtTop) {
                        ButtonBar(
                            items = listOf(Unit),
                            onClick = {
                                showScriptsDialog = true
                            },
                            photo = {
                                Icon(
                                    Icons.Outlined.HistoryEdu,
                                    null
                                )
                            },
                            title = { stringResource(R.string.scripts) },
                            itemModifier = {
                                Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .padding(horizontal = 2.pad, vertical = 1.pad)
                            }
                        )
                    }
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

                androidx.compose.animation.AnimatedVisibility(
                    showShareAThought,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    PageInput {
                        if (locationSelector.isManual) {
                            ElevatedButton(
                                elevation = ButtonDefaults.elevatedButtonElevation(2.pad),
                                onClick = {
                                    locationSelector.reset()
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                            ) {
                                Text(
                                    stringResource(R.string.reset_location),
                                    modifier = Modifier.padding(end = 1.pad)
                                )
                                Icon(Icons.Outlined.Clear, stringResource(R.string.reset_location))
                            }
                        }
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
                                    Icon(Icons.Outlined.Add, stringResource(R.string.write_a_story))
                                }
                            },
                            onAction = {
                                if (thought.isBlank()) {
                                    nav.navigate(AppNav.Write)
                                } else {
                                    scope.launch {
                                        api.createStory(Story(title = thought, content = emptyStoryContent())) {
                                            nav.navigate(AppNav.WriteStory(it.id!!))
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

fun emptyStoryContent() = listOf(
    StoryContent.Text("")
).toJsonStoryContent(json)
