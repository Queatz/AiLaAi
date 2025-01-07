package com.queatz.ailaai.message

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.ailaai.api.messageReactions
import app.ailaai.api.reactToMessage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.me
import com.queatz.ailaai.reaction.AllReactionsDialog
import com.queatz.ailaai.reaction.Reactions
import com.queatz.db.Message
import com.queatz.db.ReactBody
import com.queatz.db.Reaction
import com.queatz.db.ReactionAndPerson
import kotlinx.coroutines.launch

@Composable
fun MessageReactions(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    message: Message,
    onReactionChange: () -> Unit,
) {
    val me = me
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReactions by rememberStateOf<List<ReactionAndPerson>?>(null)

    showReactions?.let { reactions ->
        AllReactionsDialog(
            onDismissRequest = {
                showReactions = null
            },
            reactions = reactions,
            meId = me?.id,
            onRemoveReaction = { reaction ->
                scope.launch {
                    api.reactToMessage(
                        id = message.id!!,
                        react = ReactBody(
                            reaction = Reaction(
                                reaction = reaction.reaction!!
                            ),
                            remove = true
                        )
                    ) {
                        context.toast(R.string.reaction_removed)
                        onReactionChange()
                    }
                }
            }
        )
    }

    Reactions(
        reactions = message.reactions,
        modifier = modifier,
        alignment = alignment,
        showReactCustom = false,
        isSmall = true,
        onReact = { reaction ->
            scope.launch {
                api.reactToMessage(
                    id = message.id!!,
                    react = ReactBody(
                        reaction = Reaction(
                            reaction = reaction
                        )
                    )
                ) {
                    onReactionChange()
                }
            }
        },
        onRemoveReaction = { reaction ->
            scope.launch {
                api.reactToMessage(
                    id = message.id!!,
                    react = ReactBody(
                        reaction = Reaction(
                            reaction = reaction
                        ),
                        remove = true
                    )
                ) {
                    context.toast(R.string.reaction_removed)
                    onReactionChange()
                }
            }
        },
        onShowAllReactions = {
            scope.launch {
                api.messageReactions(message.id!!) {
                    showReactions = it.sortedByDescending { it.person?.id == me?.id }
                }
            }
        }
    )
}
