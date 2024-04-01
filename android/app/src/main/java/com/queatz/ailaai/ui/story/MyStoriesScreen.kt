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
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.myStories
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Story
import kotlinx.coroutines.launch

@Composable
fun MyStoriesScreen() {
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    var isLoading by rememberStateOf(true)
    var stories by remember { mutableStateOf(emptyList<Story>()) }
    var search by rememberStateOf("")
    val nav = nav

    LaunchedEffect(Unit) {
        api.myStories {
            stories = it.sortedBy { it.published ?: false }
        }
        isLoading = false
    }

    Column {
        AppHeader(
            stringResource(R.string.write),
            {
                scope.launch {
                    state.scrollToTop()
                }
            }
        )

        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                Loading()
            } else if (stories.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    EmptyText(stringResource(R.string.you_havent_written))
                }
            } else {
                val shownStories = remember(stories, search) {
                    stories.filter {
                        it.asTextContent().contains(search, ignoreCase = true)
                    }
                }
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 1.pad,
                        start = 1.pad,
                        end = 1.pad,
                        bottom = 1.pad + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.pad)
                ) {
                    items(
                        shownStories
                    ) { story ->
                        StoryCard(
                            story,
                            isLoading = false,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            if (story.published == true) {
                                nav.navigate(AppNav.Story(story.id!!))
                            } else {
                                nav.navigate(AppNav.WriteStory(story.id!!))
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
                                nav.navigate(AppNav.WriteStory(it.id!!))
                            }
                        }
                    }
                )
            }
        }
    }
}
