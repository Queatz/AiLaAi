package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.myStories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Person
import com.queatz.db.Story
import kotlinx.coroutines.launch

@Composable
fun MyStoriesScreen(navController: NavController, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    var isLoading by rememberStateOf(true)
    var stories by remember { mutableStateOf(emptyList<Story>()) }
    var search by rememberStateOf("")

    LaunchedEffect(Unit) {
        api.myStories {
            stories = it.sortedBy { it.published ?: false }
        }
        isLoading = false
    }

    Column {
        AppHeader(
            navController,
            stringResource(R.string.write),
            {
                scope.launch {
                    state.scrollToTop()
                }
            },
            me
        )

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                Loading()
            } else if (stories.isEmpty()) {
                EmptyText(stringResource(R.string.you_havent_written))
            } else {
                val shownStories = remember(stories, search) {
                    stories.filter {
                        it.textContent().contains(search, ignoreCase = true)
                    }
                }
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = PaddingDefault,
                        start = PaddingDefault,
                        end = PaddingDefault,
                        bottom = PaddingDefault + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault * 2)
                ) {
                    items(
                        shownStories
                    ) { story ->
                        StoryCard(
                            story,
                            navController,
                            isLoading = false,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            if (story.published == true) {
                                navController.navigate("story/${story.id}")
                            } else {
                                navController.navigate("write/${story.id}")
                            }
                        }
                    }
                }
            }
            PageInput {
                SearchFieldAndAction(
                    search,
                    { search = it },
                    placeholder = stringResource(R.string.search),
                    action = {
                        Icon(Icons.Outlined.Add, stringResource(R.string.write_a_story))
                    },
                    onAction = {
                        scope.launch {
                            api.createStory(Story()) {
                                navController.navigate("write/${it.id}")
                            }
                        }
                    },
                )
            }
        }
    }
}
