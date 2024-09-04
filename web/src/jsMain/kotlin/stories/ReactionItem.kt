package stories

import androidx.compose.runtime.*
import appString
import com.queatz.db.ReactionAndPerson
import com.queatz.db.ReactionCount
import components.IconButton
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r
import stories.StoryStyles.reactionSpan

@Composable
fun ReactionItem(
    reactions: List<ReactionCount>,
    onAddReaction: (String) -> Unit,
    onReactionComment: (String, String) -> Unit
) {
    var showAddReactionDialog by remember { mutableStateOf(false) }
    var selectedReactionForComment by remember { mutableStateOf<ReactionCount?>(null) }
    var commentText by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }

    Div({
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Start)
            gap(0.5.r)
        }
    }) {
        reactions.forEach { reaction ->
            Span({
                classes(reactionSpan)
            }) {
                IconButton(
                    name = "",
                    text = reaction.reaction + " " + reaction.count.toString(),
                    title = "",
                    onClick = {
                        onAddReaction(reaction.reaction)
                    }
                )
            }
        }
        IconButton(
            name = "add_reaction",
            title = appString { addReaction },
            onClick = { showAddReactionDialog = true },
            styles = {
                border {  }
            }
        )
    }

    if (showAddReactionDialog) {
        AddReactionDialog(
            onDismissRequest = {
                showAddReactionDialog = false
            },
            onAddReaction = onAddReaction
        )
    }

    selectedReactionForComment?.let { reaction ->
        Div() {
            Text(appString { addCommentToReaction })
            TextArea(value = commentText, attrs = {
                onInput { commentText = it.value }
            })
            Button(attrs = {
                onClick {
                    isSendingComment = true
                    onReactionComment(reaction.reaction, commentText)
                    selectedReactionForComment = null
                    commentText = ""
                    isSendingComment = false
                }
                disabled()
            }) {
                Text("Send")
            }
            Button(attrs = {
                onClick {
                    selectedReactionForComment = null
                    commentText = ""
                }
            }) {
                Text("Cancel")
            }
        }
    }
}