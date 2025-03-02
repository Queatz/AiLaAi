package com.queatz.ailaai.ui.story

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ailaai.api.comment
import com.queatz.ailaai.api.story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.story.editor.StoryActions
import com.queatz.db.CommentExtended
import com.queatz.db.Story
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch

@Composable
fun StoryScreen(storyId: String, commentId: String? = null) {
    // todo storyId could be a url from a deeplink
    val state = rememberLazyGridState()
    var isLoading by rememberStateOf(true)
    var story by remember { mutableStateOf<Story?>(null) }
    var contents by remember { mutableStateOf(emptyList<StoryContent>()) }
    val scope = rememberCoroutineScope()
    var showCommentReplies by rememberStateOf<CommentExtended?>(null)

    showCommentReplies?.let {
        CommentRepliesDialog(
            onDismissRequest = { showCommentReplies = null },
            comment = it,
            onCommentDeleted = {
                showCommentReplies = null
            }
        )
    }
    suspend fun reload() {
        api.story(storyId) { story = it }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        reload()
        isLoading = false
    }

    LaunchedEffect(commentId) {
        if (commentId != null) {
            api.comment(commentId) {
                showCommentReplies = it
            }
        }
    }

    LaunchedEffect(story) {
        contents = story?.let {
            it.asContents() +
                    if (it.published == true) {
                        listOf(
                            StoryContent.Reactions(it.id!!, it.reactions),
                            StoryContent.Comments(it.id!!)
                        )
                    } else {
                        emptyList()
                    }
        } ?: emptyList()
    }

    // todo use a loading/error/empty scaffold
    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Loading()
        }
        return
    }

    val nav = nav

    StoryScaffold(
        goBack = {
            nav.popBackStack()
        },
        actions = {
            if (story == null) return@StoryScaffold
            StoryTitle(state, story!!.title)
            StoryActions(storyId, story)
        }
    ) {
        StoryContents(
            source = StorySource.Story(storyId),
            content = contents,
            state = state,
            onReloadRequest = {
                scope.launch {
                    reload()
                }
            },
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
        )
    }
}
