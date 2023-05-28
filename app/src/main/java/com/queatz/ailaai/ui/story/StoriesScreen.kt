package com.queatz.ailaai.ui.story

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.api.stories
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.helpers.locationSelector
import com.queatz.ailaai.mePresence
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.LocationScaffold
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

    LaunchedEffect(Unit) {
        delay(7_000)
        mePresence.readStoriesUntilNow()
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
                me,
                showAppIcon = true
            )
        }
    ) {
        Column {
            var stories by remember { mutableStateOf(emptyList<StoryContent>()) }
            var isLoading by rememberStateOf(true)

            LaunchedEffect(geo) {
                if (geo != null) {
                    try {
                        // todo paging
                        stories = api.stories(geo!!).flatMapIndexed { index, story ->
                            (if (index > 0) listOf(StoryContent.Divider) else emptyList()) +
                                    story.asContents()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            // Ignored, geo probably changes
                        } else {
                            e.printStackTrace()
                            context.showDidntWork()
                        }
                    }
                    isLoading = false
                }
            }

            AppHeader(
                navController,
                stringResource(R.string.stories),
                {
                    scope.launch {
                        state.scrollToTop()
                    }
                },
                me,
                showAppIcon = true
            )

            if (isLoading) {
                Loading()
            } else {
                Box(modifier = Modifier.fillMaxSize()) {

                    if (stories.isEmpty()) {
                        EmptyText(stringResource(R.string.no_stories_to_read))
                    } else {
                        StoryContents(
                            stories,
                            state,
                            navController,
                            Modifier.fillMaxSize(),
                            bottomContentPadding = 80.dp
                        ) {
                            navController.navigate("story/$it")
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
                        Icon(Icons.Outlined.HistoryEdu, stringResource(R.string.write_a_story))
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
}
