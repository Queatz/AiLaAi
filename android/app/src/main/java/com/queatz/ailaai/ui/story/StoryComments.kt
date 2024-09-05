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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.comment
import app.ailaai.api.deleteComment
import app.ailaai.api.editComment
import app.ailaai.api.replyToComment
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.ifNotEmpty
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CommentTextField
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.LinkifyText
import com.queatz.ailaai.ui.components.rememberLongClickInteractionSource
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Comment
import com.queatz.db.CommentExtended
import kotlinx.coroutines.launch

@Composable
fun StoryComments(
    comments: List<CommentExtended>,
    modifier: Modifier = Modifier,
    onCommentFocused: (Boolean) -> Unit = {},
    onCommentUpdated: (comment: CommentExtended) -> Unit = {},
    max: Int? = null,
    loadRepliesInline: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val nav = nav
    val me = me

    var loadedCommentReplies by remember {
        mutableStateOf(mapOf<String, List<CommentExtended>>())
    }

    var showAll by rememberStateOf(false)
    var showCommentReplies by rememberStateOf<CommentExtended?>(null)
    val maxShown = max != null && comments.size > max && !showAll

    suspend fun loadCommentReplies(comment: String) {
        api.comment(comment) {
            loadedCommentReplies = loadedCommentReplies + (it.comment!!.id!! to it.replies!!)
        }
    }

    showCommentReplies?.let {
        DisableSelection {
            CommentRepliesDialog(
                onDismissRequest = { showCommentReplies = null },
                comment = it,
                onCommentDeleted = {
                    onCommentUpdated(showCommentReplies!!)
                    showCommentReplies = null
                }
            )
        }
    }

    fun loadReplies(comment: CommentExtended, inline: Boolean) {
        scope.launch {
            if (inline) {
                loadCommentReplies(comment.comment!!.id!!)
            } else {
                api.comment(comment.comment!!.id!!, onError = {
                    showCommentReplies = null
                }) {
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
                val replies = (loadedCommentReplies[comment.comment!!.id!!] ?: comment.replies)?.ifNotEmpty
                var showReply by rememberStateOf(false)
                var reply by rememberStateOf("")
                var isSendingReply by rememberStateOf(false)
                var showDeleteCommentDialog by rememberStateOf(false)
                var showEditCommentDialog by rememberStateOf(false)

                DisableSelection {
                    if (showDeleteCommentDialog) {
                        Alert(
                            {
                                showDeleteCommentDialog = false
                            },
                            title = stringResource(R.string.delete_comment),
                            text = stringResource(R.string.you_cannot_undo_this_comment),
                            dismissButton = stringResource(R.string.cancel),
                            confirmButton = stringResource(R.string.yes_delete),
                            confirmColor = MaterialTheme.colorScheme.error
                        ) {
                            scope.launch {
                                api.deleteComment(comment.comment!!.id!!) {
                                    onCommentUpdated(comment)
                                    showDeleteCommentDialog = false
                                }
                            }
                        }
                    }

                    if (showEditCommentDialog) {
                        TextFieldDialog(
                            {
                                showEditCommentDialog = false
                            },
                            title = stringResource(R.string.edit_comment),
                            button = stringResource(R.string.update),
                            showDismiss = true,
                            initialValue = comment.comment?.comment ?: "",
                            requireModification = true
                        ) {
                            api.editComment(comment.comment!!.id!!, Comment(comment = it)) {
                                onCommentUpdated(comment)
                                showEditCommentDialog = false
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    GroupPhoto(
                        comment.person!!.contactPhoto().inList(),
                        padding = 0.pad,
                        size = 32.dp,
                        modifier = Modifier
                            .clickable {
                                nav.appNavigate(AppNav.Profile(comment.person!!.id!!))
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
                        DisableSelection {
                            Text(
                                bulletedString(
                                    comment.comment!!.createdAt!!.timeAgo(),
                                    stringResource(R.string.tap_to_reply)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .padding(start = 1.pad)
                            )
                        }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                CommentTextField(
                                    reply,
                                    { reply = it },
                                    modifier = Modifier
                                        .weight(1f)
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
                                    reply.notBlank?.let {
                                        isSendingReply = true
                                        scope.launch {
                                            api.replyToComment(
                                                comment.comment!!.id!!,
                                                Comment(comment = it)
                                            ) {
                                                reply = ""
                                                loadCommentReplies(comment.comment!!.id!!)
                                                showReply = false
                                            }
                                            isSendingReply = false
                                        }
                                    }
                                }
                                if (comment.person?.id == me?.id) {
                                    AnimatedVisibility(reply.isEmpty()) {
                                        var showCommentMenu by rememberStateOf(false)

                                        IconButton(
                                            {
                                                showCommentMenu = true
                                            }
                                        ) {
                                            Icon(Icons.Outlined.MoreVert, null)

                                            Dropdown(showCommentMenu, { showCommentMenu = false }) {
                                                menuItem(stringResource(R.string.edit)) {
                                                    showCommentMenu = false
                                                    showEditCommentDialog = true
                                                }

                                                menuItem(stringResource(R.string.delete)) {
                                                    showCommentMenu = false
                                                    showDeleteCommentDialog = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        replies?.let { replies ->
                            StoryComments(
                                comments = replies,
                                onCommentFocused = onCommentFocused,
                                max = max?.let { it - 1 }?.coerceAtLeast(2),
                                loadRepliesInline = loadRepliesInline,
                                onCommentUpdated = { updatedComment ->
                                    scope.launch {
                                        val toComment = replies.firstOrNull { reply -> updatedComment.comment!!.id!! == reply.comment!!.id!! }
                                        if (toComment != null) {
                                            loadCommentReplies(comment.comment!!.id!!)
                                        } else {
                                            onCommentUpdated(updatedComment)
                                        }
                                    }
                                },
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
