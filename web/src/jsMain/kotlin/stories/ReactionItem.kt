package stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.components.Spacer
import appString
import com.queatz.db.ReactionCount
import components.IconButton
import format
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import qr
import r
import stories.StoryStyles.buttonRow
import stories.StoryStyles.reactionButton
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
            gap(1.px)
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
            Text("Add Comment:")
            TextArea(value = commentText, attrs = {
                onInput { e -> commentText = e.value }
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