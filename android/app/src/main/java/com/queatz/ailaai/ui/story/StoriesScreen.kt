package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.ailaai.api.myGeo
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.Story
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var cache = mutableMapOf<MainTab, List<Story>>()

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
    var tab by rememberSavableStateOf(MainTab.Friends)
    var stories by remember { mutableStateOf(cache[tab] ?: emptyList()) }
    var storyContents by remember { mutableStateOf(emptyList<StoryContent>()) }
    var isLoading by rememberStateOf(stories.isEmpty())
    val nav = nav
    val tabs = listOf(MainTab.Friends, MainTab.Local)

    LaunchedEffect(stories) {
        cache[tab] = stories
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

    LaunchedEffect(geo, tab) {
        if (geo != null) {
            api.stories(
                geo!!.toGeo(),
                public = tab == MainTab.Local,
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
                storyContents = it.flatMapIndexed { index, story ->
                    (if (index > 0) listOf(StoryContent.Divider) else emptyList()) +
                            story.asContents()
                }
                isLoading = false
            }
        }
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
        }
    ) {
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
            MainTabs(tab, { tab = it }, tabs = tabs)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .swipeMainTabs {
                        when (val it = tabs.swipe(tab, it)) {
                            is SwipeResult.Previous -> {
                                nav.navigate("explore")
                            }
                            is SwipeResult.Next -> {
                                nav.navigate("messages")
                            }
                            is SwipeResult.Select<*> -> {
                                tab = it.item as MainTab
                            }
                        }
                    }
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
                        null,
                        storyContents,
                        state,
                        Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = 640.dp)
                            .fillMaxSize(),
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
                var thought by rememberStateOf("")
                PageInput(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    SearchFieldAndAction(
                        thought,
                        { thought = it },
                        placeholder = stringResource(R.string.share_a_thought),
                        showClear = false,
                        action = {
                            if (thought.isBlank()) {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.your_stories))
                            } else {
                                Icon(Icons.Outlined.Add, stringResource(R.string.write_a_story))
                            }
                        },
                        onAction = {
                            if (thought.isBlank()) {
                                nav.navigate("write")
                            } else {
                                scope.launch {
                                    api.createStory(Story(title = thought)) {
                                        nav.navigate("write/${it.id}")
                                    }
                                }
                            }
                        },
                    )
                }
//                }
                if (locationSelector.isManual) {
                    ElevatedButton(
                        elevation = ButtonDefaults.elevatedButtonElevation(2.pad),
                        onClick = {
                            locationSelector.reset()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.pad)
                    ) {
                        Text(
                            stringResource(R.string.reset_location),
                            modifier = Modifier.padding(end = 1.pad)
                        )
                        Icon(Icons.Outlined.Clear, stringResource(R.string.reset_location))
                    }
                }
            }
        }
    }
}
