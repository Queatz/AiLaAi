package com.queatz.ailaai.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.comment
import app.ailaai.api.replyToComment
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.ifNotEmpty
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CommentTextField
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.LinkifyText
import com.queatz.ailaai.ui.components.rememberLongClickInteractionSource
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import kotlinx.coroutines.launch

@Composable
fun StoryComments(
    comments: List<CommentExtended>,
    modifier: Modifier = Modifier,
    onCommentFocused: (Boolean) -> Unit = {},
    max: Int? = null,
    loadRepliesInline: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val nav = nav

    var loadedCommentReplies by remember {
        mutableStateOf(emptyMap<String, List<CommentExtended>>())
    }

    var showAll by rememberStateOf(false)
    var showCommentReplies by rememberStateOf<CommentExtended?>(null)
    val maxShown = max != null && comments.size > max && !showAll

    suspend fun loadCommentReplies(comment: CommentExtended) {
        api.comment(comment.comment!!.id!!) {
            loadedCommentReplies = loadedCommentReplies + (it.comment!!.id!! to it.replies!!)
        }
    }

    showCommentReplies?.let {
        CommentRepliesDialog({ showCommentReplies = null }, it)
    }

    fun loadReplies(comment: CommentExtended, inline: Boolean) {
        scope.launch {
            if (inline) {
                loadCommentReplies(comment)
            } else {
                api.comment(comment.comment!!.id!!) {
                    showCommentReplies = it
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (maxShown) {
            comments.take(max!!)
        } else {
            comments
        }.forEach { comment ->
            key(comment.comment!!.id!!) {
                val showTotalReplies = comment.totalReplies!! > 0
                val replies = (comment.replies ?: loadedCommentReplies[comment.comment!!.id!!])?.ifNotEmpty
                var showReply by rememberStateOf(false)
                var reply by rememberStateOf("")
                var isSendingReply by rememberStateOf(false)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    GroupPhoto(
                        comment.person!!.contactPhoto().inList(),
                        padding = 0.pad,
                        size = 32.dp,
                        modifier = Modifier
                            .clickable {
                                nav.navigate(AppNav.Profile(comment.person!!.id!!))
                            }
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(.25f.pad),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(.25f.pad),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    showReply = !showReply
                                }
                                .padding(1.pad)
                        ) {
                            Text(
                                comment.person!!.name ?: stringResource(R.string.someone),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            LinkifyText(comment.comment!!.comment!!)
                        }
                        Text(
                            comment.comment!!.createdAt!!.timeAgo(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(start = 1.pad)
                        )

                        val focusRequester = remember { FocusRequester() }

                        LaunchedEffect(showReply) {
                            if (showReply) {
                                focusRequester.requestFocus()
                            }
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                onCommentFocused(false)
                            }
                        }

                        AnimatedVisibility(showReply) {
                            CommentTextField(
                                reply,
                                { reply = it },

                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        onCommentFocused(it.isFocused)
                                    },
                                placeholder = stringResource(
                                    R.string.reply_to_x,
                                    comment.person?.name ?: stringResource(R.string.someone)
                                ),
                                enabled = !isSendingReply,
                                onDismissRequest = {
                                    showReply = false
                                }
                            ) {
                                reply.notBlank?.let { reply ->
                                    isSendingReply = true
                                    scope.launch {
                                        api.replyToComment(
                                            comment.comment!!.id!!,
                                            Comment(comment = reply)
                                        ) {
                                            loadCommentReplies(comment)
                                            showReply = false
                                        }
                                        isSendingReply = false
                                    }
                                }
                            }
                        }
                        replies?.ifNotEmpty?.let { replies ->
                            StoryComments(
                                replies,
                                onCommentFocused = onCommentFocused,
                                max = max?.let { it - 1 }?.coerceAtLeast(2),
                                loadRepliesInline = loadRepliesInline,
                                modifier = Modifier
                                    .padding(top = 1.pad)
                            )
                        } ?: let {
                            if (showTotalReplies) {
                                DisableSelection {
                                    OutlinedButton(
                                        onClick = {},
                                        interactionSource = rememberLongClickInteractionSource(
                                            onClick = {
                                                loadReplies(comment, loadRepliesInline)

                                            },
                                            onLongClick = {
                                                loadReplies(comment, !loadRepliesInline)
                                            }
                                        ),
                                        modifier = Modifier
                                            .padding(start = 1.pad)
                                    ) {
                                        Text(
                                            pluralStringResource(
                                                R.plurals.x_replies,
                                                comment.totalReplies!!,
                                                comment.totalReplies!!
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (maxShown) {
            val remaining = comments.size - max!!
            DisableSelection {
                OutlinedButton(
                    {
                        showAll = !showAll
                    }
                ) {
                    Text(pluralStringResource(R.plurals.show_x_more_comments, remaining, remaining.format()))
                }
            }
        }
    }
}
