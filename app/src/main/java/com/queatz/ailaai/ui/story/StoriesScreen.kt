package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.R
import com.queatz.ailaai.api.myGeo
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.Story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.services.mePresence
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var tab by rememberSavableStateOf(MainTab.Friends)
    var stories by remember { mutableStateOf(emptyList<Story>()) }
    var storyContents by remember { mutableStateOf(emptyList<StoryContent>()) }
    var isLoading by rememberStateOf(true)

    LaunchedEffect(geo) {
        geo?.let {
            api.myGeo(it)
        }
    }

    LaunchedEffect(Unit) {
        delay(7_000)
        mePresence.readStoriesUntilNow()
    }

    LaunchedEffect(geo, tab) {
        if (geo != null) {
            api.stories(
                geo!!,
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
            MainTabs(tab, { tab = it }, tabs = listOf(MainTab.Friends, MainTab.Local))
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Loading(
                        modifier = Modifier
                            .padding(PaddingDefault)
                    )
                } else if (storyContents.isEmpty()) {
                    EmptyText(stringResource(R.string.no_stories_to_read))
                } else {
                    StoryContents(
                        storyContents,
                        state,
                        navController,
                        Modifier.align(Alignment.TopCenter).widthIn(max = 640.dp).fillMaxSize(),
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
