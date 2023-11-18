package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
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
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Person
import com.queatz.db.Story
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var cache = emptyList<Story>()
private var cacheTab = MainTab.Friends

@Composable
fun StoriesScreen(navController: NavHostController, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val context = LocalContext.current
    var geo by remember { mutableStateOf<LatLng?>(null) }
    val locationSelector = locationSelector(
        geo,
        { geo = it },
        navController.context as Activity
    )
    var tab by rememberSavableStateOf(cacheTab)
    var stories by remember { mutableStateOf(cache) }
    var storyContents by remember { mutableStateOf(emptyList<StoryContent>()) }
    var isLoading by rememberStateOf(stories.isEmpty())

    val tabs = listOf(MainTab.Friends, MainTab.Local)

    LaunchedEffect(stories) {
        cache = stories
    }

    LaunchedEffect(tab) {
        cacheTab = tab
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
        navController,
        appHeader = {
            AppHeader(
                navController,
                stringResource(R.string.stories),
                {},
                me
            ) {
                ScanQrCodeButton(navController)
            }
        }
    ) {
        Column {
            AppHeader(
                navController,
                stringResource(R.string.stories),
                {
                    scope.launch {
                        state.scrollToTop()
                    }
                },
                me
            ) {
                ScanQrCodeButton(navController)
            }
            MainTabs(tab, { tab = it }, tabs = tabs)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .swipeMainTabs {
                        when (val it = tabs.swipe(tab, it)) {
                            is SwipeResult.Previous -> {
                                navController.navigate("explore")
                            }
                            is SwipeResult.Next -> {
                                navController.navigate("messages")
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
                            .padding(PaddingDefault)
                    )
                } else if (storyContents.isEmpty()) {
                    EmptyText(stringResource(R.string.no_stories_to_read))
                } else {
                    StoryContents(
                        null,
                        storyContents,
                        state,
                        navController,
                        me,
                        Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = 640.dp)
                            .fillMaxSize(),
                        bottomContentPadding = 80.dp
                    ) { storyId ->
                        Row {
                            StoryActions(
                                navController,
                                storyId,
                                stories.find { it.id == storyId },
                                me,
                                showOpen = true
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        navController.navigate("write")
                    },
                    modifier = Modifier
                        .padding(
                            end = PaddingDefault * 2,
                            bottom = PaddingDefault * 2
                        )
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Outlined.Edit, stringResource(R.string.your_stories))
                }
                if (locationSelector.isManual) {
                    ElevatedButton(
                        elevation = ButtonDefaults.elevatedButtonElevation(ElevationDefault * 2),
                        onClick = {
                            locationSelector.reset()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = PaddingDefault * 2)
                    ) {
                        Text(
                            stringResource(R.string.reset_location),
                            modifier = Modifier.padding(end = PaddingDefault)
                        )
                        Icon(Icons.Outlined.Clear, stringResource(R.string.reset_location))
                    }
                }
            }
        }
    }
}
