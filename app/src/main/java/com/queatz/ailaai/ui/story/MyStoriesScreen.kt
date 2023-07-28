package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.data.Story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.api.createStory
import com.queatz.ailaai.api.myStories
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.scrollToTop
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStoriesScreen(navController: NavController, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    val context = LocalContext.current
    var isLoading by rememberStateOf(true)
    var stories by remember { mutableStateOf(emptyList<Story>()) }

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

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Loading()
            } else if (stories.isEmpty()) {
                EmptyText(stringResource(R.string.you_havent_written))
            } else {
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
                    items(stories) { story ->
                        Card(
                            onClick = {
                                if (story.published == true) {
                                    navController.navigate("story/${story.id}")
                                } else {
                                    navController.navigate("write/${story.id}")
                                }
                            },
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(PaddingDefault * 1.5f)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        story.title ?: stringResource(R.string.empty_story_name),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        stringResource(if (story.published == true) R.string.published else R.string.draft),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (story.published == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(story.textContent(), maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Outlined.ArrowForward, null)
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        api.createStory(Story()) {
                            navController.navigate("write/${it.id}")
                        }
                    }
                },
                modifier = Modifier
                    .padding(
                        end = PaddingDefault * 2,
                        bottom = PaddingDefault * 2
                    )
                    .align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Outlined.Add, stringResource(R.string.write_a_story))
            }
        }
    }
}
