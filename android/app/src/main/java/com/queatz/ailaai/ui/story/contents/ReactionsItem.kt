package com.queatz.ailaai.ui.story.contents

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.reactToStory
import com.queatz.ailaai.api.storyReactions
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.nav
import com.queatz.ailaai.reaction.AllReactionsDialog
import com.queatz.ailaai.reaction.Reactions
import com.queatz.ailaai.ui.components.CommentTextField
import com.queatz.ailaai.ui.dialogs.AddReactionDialog
import com.queatz.ailaai.ui.dialogs.ItemsPeopleDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.ReactBody
import com.queatz.db.Reaction
import com.queatz.db.ReactionAndPerson
import com.queatz.db.StoryContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun LazyGridScope.reactionsItem(
    content: StoryContent.Reactions,
    context: Context,
    scope: CoroutineScope,
    me: Person?,
    onCommentFocused: (Boolean) -> Unit,
    onReactionChange: () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val nav = nav
        var showAddReactionDialog by rememberStateOf(false)
        var showReactions by rememberStateOf<List<ReactionAndPerson>?>(null)
        var commentOnReaction by rememberStateOf<String?>(null)
        var sendComment by rememberStateOf("")
        var isSendingComment by rememberStateOf(false)

        DisableSelection {
            fun sendComment() {
                if (sendComment.isBlank()) {
                    return
                }
                isSendingComment = true
                scope.launch {
                    api.reactToStory(
                        content.story,
                        ReactBody(Reaction(reaction = commentOnReaction!!, comment = sendComment.trim()))
                    ) {
                        commentOnReaction = null
                        sendComment = ""
                        onReactionChange()
                        context.toast(R.string.comment_added)
                    }
                    isSendingComment = false
                }
            }

            if (showAddReactionDialog) {
                AddReactionDialog(
                    onDismissRequest = {
                        showAddReactionDialog = false
                    }
                ) { reaction ->
                    if (reaction.isNotBlank()) {
                        scope.launch {
                            api.reactToStory(
                                id = content.story,
                                react = ReactBody(Reaction(reaction = reaction))
                            ) {
                                commentOnReaction = reaction
                                sendComment = ""
                                onReactionChange()
                            }
                        }
                    }
                    showAddReactionDialog = false
                }
            }

            showReactions?.let { reactions ->
                AllReactionsDialog(
                    onDismissRequest = {
                        showReactions = null
                    },
                    reactions = reactions,
                    meId = me?.id,
                    onRemoveReaction = { reaction ->
                        scope.launch {
                            api.reactToStory(
                                id = content.story,
                                react = ReactBody(
                                    Reaction(
                                        reaction = reaction.reaction!!
                                    ),
                                    remove = true
                                )
                            ) {
                                commentOnReaction = null
                                sendComment = ""
                                context.toast(R.string.reaction_removed)
                                onReactionChange()
                            }
                        }
                    },
                    onEditReactionComment = { reaction ->
                        commentOnReaction = reaction.reaction!!
                        sendComment = reaction.comment.orEmpty()
                    }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                Reactions(
                    reactions = content.reactions,
                    modifier = Modifier
                        .fillMaxWidth(),
                    selected = commentOnReaction,
                    onReact = { reaction ->
                        scope.launch {
                            api.reactToStory(
                                id = content.story,
                                react = ReactBody(Reaction(reaction = reaction))
                            ) {
                                commentOnReaction = reaction
                                sendComment = ""
                                onReactionChange()
                            }
                        }
                    },
                    onReactCustom = {
                        showAddReactionDialog = true
                    },
                    onRemoveReaction = { reaction ->
                        scope.launch {
                            api.reactToStory(
                                id = content.story,
                                react = ReactBody(
                                    reaction = Reaction(
                                        reaction = reaction
                                    ),
                                    remove = true
                                )
                            ) {
                                commentOnReaction = null
                                sendComment = ""
                                context.toast(R.string.reaction_removed)
                                onReactionChange()
                            }
                        }
                    },
                    onShowAllReactions = {
                        scope.launch {
                            api.storyReactions(content.story) {
                                showReactions =
                                    it.sortedByDescending { it.person?.id == me?.id }
                            }
                        }
                    }
                )

                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(commentOnReaction) {
                    if (commentOnReaction != null) {
                        focusRequester.requestFocus()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        onCommentFocused(false)
                    }
                }

                AnimatedVisibility(commentOnReaction != null) {
                    CommentTextField(
                        sendComment,
                        { sendComment = it },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                onCommentFocused(it.isFocused)
                            },
                        placeholder = stringResource(R.string.add_a_comment_to_your_reaction),
                        enabled = !isSendingComment,
                        onDismissRequest = {
                            commentOnReaction = null
                            sendComment = ""
                        }
                    ) {
                        sendComment()
                    }
                }
            }
        }
    }
}
