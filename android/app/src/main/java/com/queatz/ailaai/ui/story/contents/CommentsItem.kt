package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.api.commentOnStory
import com.queatz.ailaai.api.storyComments
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.ui.components.CommentTextField
import com.queatz.ailaai.ui.story.StoryComments
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch

fun LazyGridScope.commentsItem(
    content: StoryContent.Comments,
    onCommentFocused: (Boolean) -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var sendComment by rememberStateOf("")
        var isSendingComment by rememberStateOf(false)

        var comments by remember(content.story) {
            mutableStateOf<List<CommentExtended>?>(null)
        }

        suspend fun reloadComments() {
            api.storyComments(content.story) {
                comments = it
            }
        }

        LaunchedEffect(content.story) {
            reloadComments()
        }

        fun sendComment() {
            if (sendComment.isBlank()) {
                return
            }

            isSendingComment = true
            scope.launch {
                api.commentOnStory(
                    content.story,
                    Comment(comment = sendComment)
                ) {
                    sendComment = ""
                    reloadComments()
                    context.toast(R.string.comment_added)
                }
                isSendingComment = false
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(1.5f.pad),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            DisposableEffect(Unit) {
                onDispose {
                    onCommentFocused(false)
                }
            }

            CommentTextField(
                sendComment,
                { sendComment = it },
                modifier = Modifier
                    .onFocusChanged {
                        onCommentFocused(it.isFocused)
                    },
                placeholder = stringResource(R.string.share_a_comment),
                enabled = !isSendingComment,
                onDismissRequest = {
                    sendComment = ""
                }
            ) {
                sendComment()
            }

            comments?.let {
                StoryComments(
                    it,
                    onCommentFocused = onCommentFocused,
                    onCommentUpdated = {
                        scope.launch {
                            reloadComments()
                        }
                    },
                    max = 2
                )
            }
        }
    }
}
