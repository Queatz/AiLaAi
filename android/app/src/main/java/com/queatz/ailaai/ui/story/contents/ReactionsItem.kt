package com.queatz.ailaai.ui.story.contents

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.reactToStory
import com.queatz.ailaai.api.storyReactions
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.formatMini
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CommentTextField
import com.queatz.ailaai.ui.components.rememberLongClickInteractionSource
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

@OptIn(ExperimentalLayoutApi::class)
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
                    {
                        showAddReactionDialog = false
                    }
                ) { reaction ->
                    scope.launch {
                        api.reactToStory(
                            content.story,
                            ReactBody(Reaction(reaction = reaction))
                        ) {
                            commentOnReaction = reaction
                            sendComment = ""
                            onReactionChange()
                        }
                    }
                    showAddReactionDialog = false
                }
            }

            showReactions?.let { reactions ->
                ItemsPeopleDialog(
                    title = stringResource(id = R.string.reactions),
                    onDismissRequest = {
                        showReactions = null
                    },
                    items = reactions,
                    key = { it.reaction!!.id!! },
                    people = { it.person!! },
                    infoFormatter = { reaction ->
                        bulletedString(
                            reaction.reaction!!.reaction!!,
                            reaction.reaction!!.createdAt!!.shortAgo(context),
                            reaction.reaction!!.comment,
                            if (reaction.person?.id == me?.id) {
                                context.getString(R.string.tap_to_edit)
                            } else {
                                null
                            }
                        )
                    },
                    itemAction = { reaction ->
                        if (reaction.person?.id == me?.id) {
                            IconButton(
                                onClick = {
                                    showReactions = null
                                    scope.launch {
                                        api.reactToStory(
                                            content.story,
                                            ReactBody(
                                                Reaction(
                                                    reaction = reaction.reaction!!.reaction!!
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
                                }
                            ) {
                                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                ) { reaction ->
                    showReactions = null
                    if (reaction.person?.id == me?.id) {
                        commentOnReaction = reaction.reaction!!.reaction!!
                        sendComment = reaction.reaction!!.comment.orEmpty()
                    } else {
                        nav.navigate(AppNav.Profile(reaction.person!!.id!!))
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
                    horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (content.reactions?.all.isNullOrEmpty()) {
                        val reaction = "❤"
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    api.reactToStory(
                                        content.story,
                                        ReactBody(Reaction(reaction = reaction))
                                    ) {
                                        commentOnReaction = reaction
                                        sendComment = ""
                                        onReactionChange()
                                    }
                                }
                            }
                        ) {
                            Text(reaction, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "0",
                                modifier = Modifier
                                    .padding(start = .5f.pad)
                            )
                        }
                    } else {
                        content.reactions!!.all.forEach { reaction ->
                            val mine = content.reactions!!.mine?.any {
                                it.reaction == reaction.reaction
                            } == true

                            key(reaction.reaction, reaction.count, mine) {
                                OutlinedButton(
                                    onClick = {},
                                    colors = if (mine) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                    border = if (commentOnReaction == reaction.reaction) {
                                        ButtonDefaults.outlinedButtonBorder.copy(
                                            width = 2.dp, brush = SolidColor(
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonBorder
                                    },
                                    interactionSource = rememberLongClickInteractionSource(
                                        onClick = {
                                            scope.launch {
                                                if (mine) {
                                                    api.storyReactions(content.story) {
                                                        showReactions =
                                                            it.sortedByDescending { it.person?.id == me?.id }
                                                    }
                                                } else {
                                                    api.reactToStory(
                                                        content.story,
                                                        ReactBody(
                                                            Reaction(
                                                                reaction = reaction.reaction
                                                            )
                                                        )
                                                    ) {
                                                        commentOnReaction = reaction.reaction
                                                        sendComment = ""
                                                        onReactionChange()
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        scope.launch {
                                            if (mine) {
                                                api.reactToStory(
                                                    content.story,
                                                    ReactBody(
                                                        Reaction(
                                                            reaction = reaction.reaction
                                                        ),
                                                        remove = true
                                                    )
                                                ) {
                                                    commentOnReaction = null
                                                    sendComment = ""
                                                    context.toast(R.string.reaction_removed)
                                                    onReactionChange()
                                                }
                                            } else {
                                                api.storyReactions(content.story) {
                                                    showReactions = it.sortedByDescending { it.person?.id == me?.id }
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        reaction.reaction,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                    )
                                    Text(
                                        reaction.count.formatMini(),
                                        style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .padding(start = .5f.pad)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showAddReactionDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Outlined.AddReaction,
                            null,
                            modifier = Modifier
                                .fillMaxHeight()
                        )
                    }
                }

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
