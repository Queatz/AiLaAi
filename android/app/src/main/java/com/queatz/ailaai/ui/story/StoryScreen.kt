package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.api.story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.db.Story
import com.queatz.db.StoryContent

@Composable
fun StoryScreen(storyId: String) {
    // todo storyId could be a url from a deeplink
    val state = rememberLazyGridState()
    var isLoading by rememberStateOf(true)
    var story by remember { mutableStateOf<Story?>(null) }
    var contents by remember { mutableStateOf(emptyList<StoryContent>()) }

    LaunchedEffect(Unit) {
        isLoading = true
        api.story(storyId) { story = it }
        isLoading = false
    }

    LaunchedEffect(story) {
        contents = story?.let {
            it.asContents() +
                    listOf(StoryContent.Reactions(it.id!!, it.reactions))
        } ?: emptyList()
    }

    // todo use a loading/error/empty scaffold
    if (isLoading) {
        Loading()
        return
    }

    val nav = nav

    StoryScaffold(
        {
            nav.popBackStack()
        },
        actions = {
            if (story == null) return@StoryScaffold
            StoryTitle(state, story)
            StoryActions(storyId, story)
        }
    ) {
        StoryContents(
            StorySource.Story(storyId),
            contents,
            state,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
        )
    }
}
